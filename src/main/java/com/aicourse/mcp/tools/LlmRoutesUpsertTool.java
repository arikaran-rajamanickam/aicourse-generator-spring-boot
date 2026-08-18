package com.aicourse.mcp.tools;

import com.aicourse.ai.dto.LlmRouteResponse;
import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.dto.LlmRouteUpsertToolInput;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LlmRoutesUpsertTool implements McpToolHandler {

    private final LlmAdminService llmAdminService;
    private final ObjectMapper objectMapper;

    public LlmRoutesUpsertTool(LlmAdminService llmAdminService, ObjectMapper objectMapper) {
        this.llmAdminService = llmAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "llm.routes.upsert";
    }

    @Override
    public String description() {
        return "Update workload to provider routing (admin-only)";
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) {
        requireAdmin(context);
        LlmRouteUpsertToolInput payload = objectMapper.convertValue(input, LlmRouteUpsertToolInput.class);
        if (payload.getWorkload() == null) {
            throw new IllegalArgumentException("workload is required");
        }
        if (payload.getProviderCode() == null || payload.getProviderCode().isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }

        LlmRouteResponse response = llmAdminService.upsertRoute(payload.toRequest());
        return objectMapper.valueToTree(response);
    }

    private void requireAdmin(McpExecutionContext context) {
        if (context.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }
}

