package com.aicourse.mcp.tools;

import com.aicoach.dto.AiCoachResponse;
import com.aicoach.service.AiCoachService;
import com.aicourse.mcp.dto.CoachRespondToolInput;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CoachRespondTool implements McpToolHandler {

    private final AiCoachService aiCoachService;
    private final ObjectMapper objectMapper;

    public CoachRespondTool(AiCoachService aiCoachService, ObjectMapper objectMapper) {
        this.aiCoachService = aiCoachService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "coach.respond";
    }

    @Override
    public String description() {
        return "Get an AI coach response grounded to the current course context";
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) throws Exception {
        CoachRespondToolInput payload = objectMapper.treeToValue(input, CoachRespondToolInput.class);
        if (payload.getCourseId() == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (payload.getMessage() == null || payload.getMessage().isBlank()) {
            throw new IllegalArgumentException("message is required");
        }

        AiCoachResponse response = aiCoachService.respond(context.userId(), context.role(), payload.toCoachRequest());
        return objectMapper.valueToTree(response);
    }
}

