package com.aicourse.mcp.tools;

import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LlmProvidersListTool implements McpToolHandler {

    private final LlmAdminService llmAdminService;
    private final ObjectMapper objectMapper;

    public LlmProvidersListTool(LlmAdminService llmAdminService, ObjectMapper objectMapper) {
        this.llmAdminService = llmAdminService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "llm.providers.list";
    }

    @Override
    public String description() {
        return "List LLM providers and key health information (admin-only)";
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) {
        requireAdmin(context);
        return objectMapper.valueToTree(llmAdminService.getProviders());
    }

    private void requireAdmin(McpExecutionContext context) {
        if (context.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin access required");
        }
    }
}

