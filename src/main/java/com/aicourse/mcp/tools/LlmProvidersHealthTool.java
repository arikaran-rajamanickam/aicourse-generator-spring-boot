package com.aicourse.mcp.tools;

import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LlmProvidersHealthTool implements McpToolHandler {

    private final LlmAdminService llmAdminService;
    private final ObjectMapper objectMapper;

    public LlmProvidersHealthTool(LlmAdminService llmAdminService, ObjectMapper objectMapper) {
        this.llmAdminService = llmAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "llm.providers.health";
    }

    @Override
    public String description() {
        return "List LLM provider health snapshots (admin-only)";
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) {
        requireAdmin(context);
        return objectMapper.valueToTree(llmAdminService.getProviderHealthSnapshots());
    }

    private void requireAdmin(McpExecutionContext context) {
        if (context.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }
}

