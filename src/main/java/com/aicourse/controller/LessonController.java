package com.aicourse.controller;

import com.aicourse.mcp.service.McpFacadeService;
import com.aicourse.model.Lesson;
import com.aicourse.service.courses.CourseService;
import com.aicourse.service.courses.impl.LessonServiceImpl;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/courses")
public class LessonController {

    private static final Logger LOGGER = Logger.getLogger(LessonController.class.getName());

    @Autowired
    private LessonServiceImpl lessonServiceImpl;

    @Autowired
    private CourseService courseService;

    @Autowired
    private McpFacadeService mcpFacadeService;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    @PostMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}/generate")
    public ResponseEntity<Lesson> generateLesson(@PathVariable Long courseId, @PathVariable Long moduleId,
                                                 @PathVariable Long lessonId, Authentication authentication) throws Exception {
        LOGGER.log(Level.INFO, "Request received to generate lesson ID: {0} for module ID: {1} in course ID: {2}",
                new Object[]{lessonId, moduleId, courseId});
        try {

            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Lesson lesson;
            if (mcpEnabled) {
                lesson = mcpFacadeService.generateLesson(courseId, moduleId, lessonId, principal);
            } else {
                Long userId = principal.getUser().getId();
                lesson = lessonServiceImpl.generateLessonContent(courseId, moduleId, lessonId, userId);
            }

            LOGGER.log(Level.INFO, "Lesson ID: {0} generated successfully", new Object[]{lessonId});
            return ResponseEntity.ok(lesson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating lesson ID: {0}: {1}", new Object[]{lessonId, e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/{courseId}/modules/{moduleId}/lessons/batch-generate")
    public ResponseEntity<java.util.List<Lesson>> batchGenerateLessons(@PathVariable Long courseId, @PathVariable Long moduleId,
                                                 @RequestParam(defaultValue = "3") int limit, Authentication authentication) throws Exception {
        LOGGER.log(Level.INFO, "Request received to batch generate up to {0} lessons for module ID: {1} in course ID: {2}",
                new Object[]{limit, moduleId, courseId});
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long userId = principal.getUser().getId();
            // Note: MCP is currently not supporting batch directly, so we use the default service
            java.util.List<Lesson> generatedLessons = lessonServiceImpl.batchGenerateLessonsForModule(courseId, moduleId, limit, userId);

            LOGGER.log(Level.INFO, "Batch generation completed. {0} lessons generated.", new Object[]{generatedLessons.size()});
            return ResponseEntity.ok(generatedLessons);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error batch generating lessons for module ID: {0}: {1}", new Object[]{moduleId, e.getMessage()});
            throw e;
        }
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<Lesson> getLesson(@PathVariable Long id, Authentication authentication) throws Exception {
        LOGGER.log(Level.INFO, "Fetching lesson details for ID: {0}", new Object[]{id});
        try {
            if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
            Lesson lesson = lessonServiceImpl.getLessonForUser(id, userId);
            LOGGER.log(Level.INFO, "Lesson details retrieved for ID: {0}", new Object[]{id});
            return ResponseEntity.ok(lesson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching lesson ID: {0}: {1}", new Object[]{id, e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/{courseId}/modules/{moduleId}/lessons")
    public ResponseEntity<Lesson> addLesson(@PathVariable Long courseId, @PathVariable Long moduleId, @RequestBody Map<String, String> payload) throws Exception {
        String title = payload.getOrDefault("title", "New Lesson");
        Lesson lesson = courseService.addLesson(moduleId, title);
        return ResponseEntity.ok(lesson);
    }

    @PutMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<Void>> renameLesson(@PathVariable Long courseId, @PathVariable Long moduleId, @PathVariable Long lessonId, @RequestBody Map<String, String> payload) throws Exception {
        String title = payload.get("title");
        courseService.renameLesson(lessonId, title);
        return ResponseEntity.ok(ApiResponse.success("Lesson renamed", null));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long courseId, @PathVariable Long moduleId, @PathVariable Long lessonId) throws Exception {
        courseService.deleteLesson(lessonId);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted", null));
    }
}
