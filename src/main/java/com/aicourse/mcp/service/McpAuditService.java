package com.aicourse.mcp.service;

import com.aicourse.mcp.dto.McpAuditLogPageResponse;
import com.aicourse.mcp.dto.McpAuditLogResponse;
import com.aicourse.mcp.model.McpAuditLog;
import com.aicourse.mcp.repo.McpAuditLogRepo;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class McpAuditService {

    private static final Logger LOGGER = Logger.getLogger(McpAuditService.class.getName());

    private final McpAuditLogRepo auditLogRepo;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public McpAuditService(McpAuditLogRepo auditLogRepo,
                           NamedParameterJdbcTemplate jdbcTemplate) {
        this.auditLogRepo = auditLogRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String requestId,
                       String tool,
                       Long userId,
                       String userRole,
                       int inputSize,
                       String status,
                       long latencyMs,
                       String errorMessage,
                       String responseBody) {
        try {
            McpAuditLog log = new McpAuditLog();
            log.setRequestId(requestId);
            log.setTool(tool);
            log.setUserId(userId);
            log.setUserRole(userRole);
            log.setInputSize(Math.max(0, inputSize));
            log.setStatus(status);
            log.setLatencyMs(Math.max(0L, latencyMs));
            log.setErrorMessage(errorMessage);
            log.setResponseBody(responseBody);
            auditLogRepo.save(log);
        } catch (Exception ex) {
            // Audit persistence must never break tool execution.
            LOGGER.log(Level.WARNING, "Failed to persist MCP audit log: {0}", ex.getMessage());
        }
    }

    public McpAuditLogPageResponse search(String tool,
                                          String status,
                                          OffsetDateTime from,
                                          OffsetDateTime to,
                                          int page,
                                          int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));

        StringBuilder where = new StringBuilder(" where 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (tool != null && !tool.isBlank()) {
            where.append(" and tool = :tool");
            params.addValue("tool", tool.trim());
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        if (from != null) {
            where.append(" and created_at >= :from");
            params.addValue("from", from);
        }
        if (to != null) {
            where.append(" and created_at <= :to");
            params.addValue("to", to);
        }

        String countSql = "select count(*) from mcp_audit_logs" + where;
        Long totalItems = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long safeTotalItems = totalItems == null ? 0L : totalItems;

        String dataSql = "select id, request_id, tool, user_id, user_role, input_size, status, latency_ms, error_message, response_body, created_at " +
                "from mcp_audit_logs" + where + " order by created_at desc limit :limit offset :offset";
        params.addValue("limit", safeSize);
        params.addValue("offset", (long) safePage * safeSize);

        List<McpAuditLogResponse> items = jdbcTemplate.query(dataSql, params, (rs, rowNum) -> {
            McpAuditLogResponse response = new McpAuditLogResponse();
            response.setId(rs.getLong("id"));
            response.setRequestId(rs.getString("request_id"));
            response.setTool(rs.getString("tool"));
            response.setUserId(rs.getObject("user_id", Long.class));
            response.setUserRole(rs.getString("user_role"));
            response.setInputSize(rs.getObject("input_size", Integer.class));
            response.setStatus(rs.getString("status"));
            response.setLatencyMs(rs.getObject("latency_ms", Long.class));
            response.setErrorMessage(rs.getString("error_message"));
            response.setResponseBody(rs.getString("response_body"));
            response.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
            return response;
        });

        int totalPages = safeTotalItems == 0 ? 0 : (int) Math.ceil((double) safeTotalItems / safeSize);

        McpAuditLogPageResponse response = new McpAuditLogPageResponse();
        response.setItems(items);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalItems(safeTotalItems);
        response.setTotalPages(totalPages);
        return response;
    }

    public LinkedHashMap<String, List<String>> getFilterOptions() {
        LinkedHashMap<String, List<String>> response = new LinkedHashMap<>();
        response.put("tools", auditLogRepo.findDistinctTools());
        response.put("statuses", auditLogRepo.findDistinctStatuses());
        return response;
    }

    // Mapping is handled directly in SQL row mapper for paged queries.
}



