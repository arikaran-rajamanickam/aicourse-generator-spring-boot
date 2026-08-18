package com.aicourse.mcp.repo;

import com.aicourse.mcp.model.McpAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface McpAuditLogRepo extends JpaRepository<McpAuditLog, Long>, JpaSpecificationExecutor<McpAuditLog> {

    @Query("select distinct m.tool from McpAuditLog m order by m.tool asc")
    List<String> findDistinctTools();

    @Query("select distinct m.status from McpAuditLog m order by m.status asc")
    List<String> findDistinctStatuses();
}


