package com.aicourse.mcp.service;

import com.aicoach.dto.AiCoachRequest;
import com.aicoach.dto.AiCoachResponse;
import com.aicourse.mcp.dto.CoachRespondToolInput;
import com.aicourse.mcp.dto.LessonGenerateToolInput;
import com.aicourse.mcp.dto.McpToolRequest;
import com.aicourse.mcp.dto.McpToolResponse;
import com.aicourse.model.Lesson;
import com.auth.model.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class McpFacadeService {

    public static final String TOOL_LESSON_GENERATE = "lesson.generate";
    public static final String TOOL_COACH_RESPOND = "coach.respond";

    private static final Logger LOGGER = Logger.getLogger(McpFacadeService.class.getName());

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final McpAuditService mcpAuditService;

    public McpFacadeService(McpToolRegistry toolRegistry,
                            ObjectMapper objectMapper,
                            McpAuditService mcpAuditService) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.mcpAuditService = mcpAuditService;
    }

    public McpToolResponse execute(McpToolRequest request, UserPrincipal principal) {
        long startedAtNanos = System.nanoTime();
        String requestId = UUID.randomUUID().toString();

        if (request == null || request.getTool() == null || request.getTool().isBlank()) {
            return McpToolResponse.failure(null, "tool is required");
        }
        if (principal == null) {
            return McpToolResponse.failure(request.getTool(), "authentication is required");
        }

        String tool = request.getTool();
        JsonNode inputNode = request.getInput() == null ? objectMapper.createObjectNode() : request.getInput();
        long userId = principal.getUser().getId() == null ? -1L : principal.getUser().getId();

        LOGGER.log(Level.INFO,
                "mcp.audit requestId={0} tool={1} userId={2} role={3} inputSize={4}",
                new Object[]{requestId, tool, userId, principal.getUser().getRoles(), inputNode.toString().length()});

        try {
            McpExecutionContext context = new McpExecutionContext(
                    principal.getUser().getId(),
                    principal.getUser().getRoles()
            );

            JsonNode data = toolRegistry.getRequired(tool).execute(inputNode, context);
            long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            LOGGER.log(Level.INFO,
                    "mcp.audit requestId={0} tool={1} status=SUCCESS latencyMs={2}",
                    new Object[]{requestId, tool, elapsedMs});
            mcpAuditService.record(
                    requestId,
                    tool,
                    userId,
                    principal.getUser().getRoles().name(),
                    inputNode.toString().length(),
                    "SUCCESS",
                    elapsedMs,
                    null,
                    data != null ? data.toString() : null
            );
            return McpToolResponse.success(tool, data);
        } catch (Exception ex) {
            long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            LOGGER.log(Level.WARNING,
                    "mcp.audit requestId={0} tool={1} status=FAILURE latencyMs={2} error={3}",
                    new Object[]{requestId, tool, elapsedMs, ex.getMessage()});
            mcpAuditService.record(
                    requestId,
                    tool,
                    userId,
                    principal.getUser().getRoles().name(),
                    inputNode.toString().length(),
                    "FAILURE",
                    elapsedMs,
                    ex.getMessage(),
                    null
            );
            return McpToolResponse.failure(tool, ex.getMessage());
        }
    }

    public Lesson generateLesson(Long courseId, Long moduleId, Long lessonId, UserPrincipal principal) throws Exception {
        LessonGenerateToolInput input = new LessonGenerateToolInput();
        input.setCourseId(courseId);
        input.setModuleId(moduleId);
        input.setLessonId(lessonId);

        McpToolRequest request = new McpToolRequest();
        request.setTool(TOOL_LESSON_GENERATE);
        request.setInput(objectMapper.valueToTree(input));

        McpToolResponse response = execute(request, principal);
        if (!response.isSuccess()) {
            throw new IllegalStateException(response.getError());
        }
        return objectMapper.treeToValue(response.getData(), Lesson.class);
    }

    public AiCoachResponse coachRespond(AiCoachRequest coachRequest, UserPrincipal principal) throws Exception {
        CoachRespondToolInput input = objectMapper.convertValue(coachRequest, CoachRespondToolInput.class);

        McpToolRequest request = new McpToolRequest();
        request.setTool(TOOL_COACH_RESPOND);
        request.setInput(objectMapper.valueToTree(input));

        McpToolResponse response = execute(request, principal);
        if (!response.isSuccess()) {
            throw new IllegalStateException(response.getError());
        }
        return objectMapper.treeToValue(response.getData(), AiCoachResponse.class);
    }
}



