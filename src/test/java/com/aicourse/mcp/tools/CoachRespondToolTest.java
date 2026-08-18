package com.aicourse.mcp.tools;

import com.aicoach.dto.AiCoachResponse;
import com.aicoach.service.AiCoachService;
import com.aicourse.mcp.service.McpExecutionContext;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CoachRespondToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeRequiresCourseId() throws Exception {
        AiCoachService aiCoachService = mock(AiCoachService.class);
        CoachRespondTool tool = new CoachRespondTool(aiCoachService, objectMapper);

        ObjectNode input = objectMapper.createObjectNode();
        input.put("message", "hello");

        assertThrows(IllegalArgumentException.class, () ->
                tool.execute(input, new McpExecutionContext(10L, UserRole.USER)));
        verifyNoInteractions(aiCoachService);
    }

    @Test
    void executeDelegatesToCoachService() throws Exception {
        AiCoachService aiCoachService = mock(AiCoachService.class);
        CoachRespondTool tool = new CoachRespondTool(aiCoachService, objectMapper);

        when(aiCoachService.respond(eq(10L), eq(UserRole.USER), any())).thenReturn(new AiCoachResponse());

        ObjectNode input = objectMapper.createObjectNode();
        input.put("courseId", 1001L);
        input.put("message", "Explain polymorphism");

        tool.execute(input, new McpExecutionContext(10L, UserRole.USER));

        verify(aiCoachService, times(1)).respond(eq(10L), eq(UserRole.USER), any());
    }
}

