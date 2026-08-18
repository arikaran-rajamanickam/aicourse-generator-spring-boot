package com.aicourse.mcp.service;

import com.aicourse.mcp.dto.McpToolRequest;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class McpFacadeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeReturnsFailureWhenToolUnsupported() {
        McpToolHandler handler = new McpToolHandler() {
            @Override
            public String toolName() {
                return "lesson.generate";
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode execute(com.fasterxml.jackson.databind.JsonNode input, McpExecutionContext context) {
                return objectMapper.createObjectNode().put("ok", true);
            }
        };

        McpToolRegistry registry = new McpToolRegistry(List.of(handler));
        McpAuditService auditService = mock(McpAuditService.class);
        McpFacadeService service = new McpFacadeService(registry, objectMapper, auditService);

        McpToolRequest request = new McpToolRequest();
        request.setTool("missing.tool");
        request.setInput(objectMapper.createObjectNode());

        var response = service.execute(request, userPrincipal(7L, UserRole.USER));

        assertFalse(response.isSuccess());
        assertEquals("missing.tool", response.getTool());
        assertTrue(response.getError().contains("Unsupported MCP tool"));
        verify(auditService, times(1)).record(any(), any(), any(), any(), anyInt(), eq("FAILURE"), anyLong(), any());
    }

    @Test
    void executeReturnsSuccessForRegisteredTool() {
        McpToolHandler handler = new McpToolHandler() {
            @Override
            public String toolName() {
                return "coach.respond";
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode execute(com.fasterxml.jackson.databind.JsonNode input, McpExecutionContext context) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("userId", context.userId());
                return node;
            }
        };

        McpToolRegistry registry = new McpToolRegistry(List.of(handler));
        McpAuditService auditService = mock(McpAuditService.class);
        McpFacadeService service = new McpFacadeService(registry, objectMapper, auditService);

        McpToolRequest request = new McpToolRequest();
        request.setTool("coach.respond");
        request.setInput(objectMapper.createObjectNode());

        var response = service.execute(request, userPrincipal(9L, UserRole.USER));

        assertTrue(response.isSuccess());
        assertEquals(9L, response.getData().path("userId").asLong());
        verify(auditService, times(1)).record(any(), any(), any(), any(), anyInt(), eq("SUCCESS"), anyLong(), isNull());
    }

    private UserPrincipal userPrincipal(Long userId, UserRole role) {
        Users user = new Users();
        user.setId(userId);
        user.setRoles(role);
        return new UserPrincipal(user);
    }
}


