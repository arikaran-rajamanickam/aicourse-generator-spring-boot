package com.aicourse.mcp.tools;

import com.aicourse.mcp.service.McpExecutionContext;
import com.aicourse.model.Lesson;
import com.aicourse.service.courses.impl.LessonServiceImpl;
import com.auth.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LessonGenerateToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executeRequiresIds() throws Exception {
        LessonServiceImpl lessonService = mock(LessonServiceImpl.class);
        LessonGenerateTool tool = new LessonGenerateTool(lessonService, objectMapper);

        ObjectNode input = objectMapper.createObjectNode();

        assertThrows(IllegalArgumentException.class, () ->
                tool.execute(input, new McpExecutionContext(10L, UserRole.USER)));
        verifyNoInteractions(lessonService);
    }

    @Test
    void executeDelegatesToLessonService() throws Exception {
        LessonServiceImpl lessonService = mock(LessonServiceImpl.class);
        LessonGenerateTool tool = new LessonGenerateTool(lessonService, objectMapper);

        Lesson lesson = new Lesson();
        lesson.setId(3001L);
        lesson.setTitle("Test");

        when(lessonService.generateLessonContent(1001L, 2001L, 3001L, 99L)).thenReturn(lesson);

        ObjectNode input = objectMapper.createObjectNode();
        input.put("courseId", 1001L);
        input.put("moduleId", 2001L);
        input.put("lessonId", 3001L);

        tool.execute(input, new McpExecutionContext(99L, UserRole.USER));

        verify(lessonService, times(1)).generateLessonContent(eq(1001L), eq(2001L), eq(3001L), eq(99L));
    }
}

