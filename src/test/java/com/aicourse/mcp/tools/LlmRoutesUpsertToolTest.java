package com.aicourse.mcp.tools;

import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.dto.LlmRouteResponse;
import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LlmRoutesUpsertToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeRequiresAdminRole() {
        LlmAdminService llmAdminService = mock(LlmAdminService.class);
        LlmRoutesUpsertTool tool = new LlmRoutesUpsertTool(llmAdminService, objectMapper);

        ObjectNode input = objectMapper.createObjectNode();
        input.put("workload", "AI_COACH");
        input.put("providerCode", "gemini");

        assertThrows(IllegalArgumentException.class, () ->
                tool.execute(input, new McpExecutionContext(10L, UserRole.USER)));
        verifyNoInteractions(llmAdminService);
    }

    @Test
    void executeDelegatesToAdminService() {
        LlmAdminService llmAdminService = mock(LlmAdminService.class);
        LlmRouteResponse routeResponse = new LlmRouteResponse();
        routeResponse.setWorkload(AiWorkload.AI_COACH);
        routeResponse.setProviderCode("gemini");
        when(llmAdminService.upsertRoute(any())).thenReturn(routeResponse);

        LlmRoutesUpsertTool tool = new LlmRoutesUpsertTool(llmAdminService, objectMapper);

        ObjectNode input = objectMapper.createObjectNode();
        input.put("workload", "AI_COACH");
        input.put("providerCode", "gemini");

        tool.execute(input, new McpExecutionContext(1L, UserRole.ADMIN));

        verify(llmAdminService, times(1)).upsertRoute(any());
    }
}

