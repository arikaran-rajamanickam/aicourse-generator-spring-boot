package com.aicourse.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpToolHandler {

    String toolName();

    String description();

    default com.fasterxml.jackson.databind.JsonNode inputSchema() {
        return null;
    }

    JsonNode execute(JsonNode input, McpExecutionContext context) throws Exception;
}

