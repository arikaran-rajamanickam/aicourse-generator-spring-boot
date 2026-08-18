package com.aicourse.mcp.tools;

import com.aicourse.mcp.dto.LessonGenerateToolInput;
import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.mcp.service.McpToolHandler;
import com.aicourse.model.Lesson;
import com.aicourse.service.courses.impl.LessonServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class LessonGenerateTool implements McpToolHandler {

    private final LessonServiceImpl lessonService;
    private final ObjectMapper objectMapper;

    public LessonGenerateTool(LessonServiceImpl lessonService, ObjectMapper objectMapper) {
        this.lessonService = lessonService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toolName() {
        return "lesson.generate";
    }

    @Override
    public String description() {
        return "Generate structured lesson blocks for a course lesson";
    }

    @Override
    public JsonNode inputSchema() {
        var properties = objectMapper.createObjectNode();
        properties.set("courseId", objectMapper.createObjectNode().put("type", "number"));
        properties.set("moduleId", objectMapper.createObjectNode().put("type", "number"));
        properties.set("lessonId", objectMapper.createObjectNode().put("type", "number"));

        return objectMapper.createObjectNode()
                .put("type", "object")
                .set("properties", properties);
    }

    @Override
    public JsonNode execute(JsonNode input, McpExecutionContext context) throws Exception {
        LessonGenerateToolInput payload = objectMapper.treeToValue(input, LessonGenerateToolInput.class);
        if (payload.getCourseId() == null || payload.getModuleId() == null || payload.getLessonId() == null) {
            throw new IllegalArgumentException("courseId, moduleId and lessonId are required");
        }

        Lesson lesson = lessonService.generateLessonContent(
                payload.getCourseId(),
                payload.getModuleId(),
                payload.getLessonId(),
                context.userId()
        );
        return objectMapper.valueToTree(lesson);
    }
}

