package com.aicourse.mcp.tools;

import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LlmRoutesListTool implements McpToolHandler {

    private final LlmAdminService llmAdminService;
    private final ObjectMapper objectMapper;

    public LlmRoutesListTool(LlmAdminService llmAdminService, ObjectMapper objectMapper) {
        this.llmAdminService = llmAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "llm.routes.list";
    }

    @Override
    public String description() {
        return "List LLM workload routes (admin-only)";
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) {
        requireAdmin(context);
        return objectMapper.valueToTree(llmAdminService.getRoutes());
    }

    private void requireAdmin(McpExecutionContext context) {
        if (context.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }
}

