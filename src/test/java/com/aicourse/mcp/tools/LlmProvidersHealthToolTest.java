package com.aicourse.mcp.tools;

import com.aicourse.ai.service.LlmAdminService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LlmProvidersHealthToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeRequiresAdminRole() {
        LlmAdminService llmAdminService = mock(LlmAdminService.class);
        LlmProvidersHealthTool tool = new LlmProvidersHealthTool(llmAdminService, objectMapper);

        ObjectNode input = objectMapper.createObjectNode();

        assertThrows(IllegalArgumentException.class, () ->
                tool.execute(input, new McpExecutionContext(10L, UserRole.USER)));
        verifyNoInteractions(llmAdminService);
    }

    @Test
    void executeCallsAdminServiceForAdminRole() {
        LlmAdminService llmAdminService = mock(LlmAdminService.class);
        when(llmAdminService.getProviderHealthSnapshots()).thenReturn(List.of());

        LlmProvidersHealthTool tool = new LlmProvidersHealthTool(llmAdminService, objectMapper);

        tool.execute(objectMapper.createObjectNode(), new McpExecutionContext(1L, UserRole.ADMIN));

        verify(llmAdminService, times(1)).getProviderHealthSnapshots();
    }
}

