package com.aicourse.service.courses.impl;

import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.service.AiDynamicGateway;
import com.aicourse.model.Lesson;
import com.aicourse.model.Module;
import com.aicourse.repo.LessonRepo;
import com.aicourse.service.courses.LessonPromptBuilder;
import com.aicourse.service.courses.LessonService;
import com.aicourse.utils.json.JsonParserUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.leaderboard.model.impl.UserStatsService;
import com.sharing.service.SharedCourseAccessGuard;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LessonServiceImpl implements LessonService {

    private static final Logger LOGGER = Logger.getLogger(LessonServiceImpl.class.getName());

    @Autowired
    private LessonRepo lessonRepo;

    @Autowired
    private AiDynamicGateway aiDynamicGateway;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private SharedCourseAccessGuard sharedCourseAccessGuard;

    @Override
    @Transactional
    public Lesson generateLessonContent(Long courseId, Long moduleId, Long lessonId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Generating content for Lesson ID: {0} (Module ID: {1}, Course ID: {2})",
                new Object[]{lessonId, moduleId, courseId});

        if (userId != null) {
            sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        }

        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> {
                    LOGGER.log(Level.SEVERE, "Lesson not found with id: {0}", new Object[]{lessonId});
                    return new RuntimeException("Lesson not found with id: " + lessonId);
                });

        Module module = lesson.getModule();

        if (!module.getId().equals(moduleId)) {
            LOGGER.log(Level.SEVERE, "Lesson {0} does not belong to Module {1}", new Object[]{lessonId, moduleId});
            throw new RuntimeException("Lesson " + lessonId + " does not belong to Module " + moduleId);
        }

        if (!module.getCourse().getId().equals(courseId)) {
            LOGGER.log(Level.SEVERE, "Module {0} does not belong to Course {1}", new Object[]{moduleId, courseId});
            throw new RuntimeException("Module " + moduleId + " does not belong to Course " + courseId);
        }

        String courseTitle = module.getCourse().getTitle();
        String moduleTitle = module.getTitle();
        String lessonTitle = lesson.getTitle();

        String prompt = new LessonPromptBuilder()
                .lessonTitle(lessonTitle)
                .courseTitle(courseTitle)
                .moduleTitle(moduleTitle)
                .build();

        try {
            LOGGER.log(Level.INFO, "Sending prompt to AI for block-based lesson ''{0}''", new Object[]{lessonTitle});
            String response = aiDynamicGateway.getResponse(AiWorkload.LESSON_GENERATION, prompt);

            String cleanJson = JsonParserUtil.extractRawJson(response);

            // Validate JSON before parsing
            if (!JsonParserUtil.isValidJson(cleanJson)) {
                LOGGER.log(Level.SEVERE, "AI response is not valid JSON for lesson: {0}", new Object[]{lessonTitle});
                throw new IllegalArgumentException("AI generated invalid JSON content for lesson: " + lessonTitle);
            }
            
            JsonNode contentJson = JsonParserUtil.parseStringToJsonObject(cleanJson);
            
            // If the root is an object and contains a "blocks" array, extract it
            JsonNode blocksArray = contentJson;
            if (contentJson.isObject() && contentJson.has("blocks")) {
                blocksArray = contentJson.get("blocks");
            }

            if (!blocksArray.isArray()) {
                throw new IllegalArgumentException("AI content must be a JSON array of lesson blocks");
            }

            lesson.setContent(blocksArray);
            lesson.setEnriched(true);

            // Optionally set estimated minutes based on block count
            lesson.setEstimatedMinutes(contentJson.size() * 2); 

            if (userId != null) {
                userStatsService.recordLessonCompleted(userId, courseId);
            }

            return lessonRepo.save(lesson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate lesson content", e);
            throw e;
        }
    }

    @Transactional
    public Lesson saveLessonContent(Long lessonId, String contentMd, Long userId) throws Exception {
        Lesson lesson = lessonRepo.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        sharedCourseAccessGuard.assertContentAccessAllowed(lesson.getModule().getCourse().getId(), userId);
        lesson.setContentMd(contentMd);
        return lessonRepo.save(lesson);
    }

    @Override
    @Transactional
    public java.util.List<com.aicourse.dto.GenerationLog> enrichPendingLessonsLimited() throws Exception {
        return enrichPendingLessonsLimited(2);
    }

    @Override
    @Transactional
    public java.util.List<com.aicourse.dto.GenerationLog> enrichPendingLessonsLimited(int batchSize) throws Exception {
        LOGGER.log(Level.FINE, "Checking for pending lessons to enrich...");
        List<Lesson> lessons = lessonRepo.findNextPendingLessons(batchSize);

        if (lessons.isEmpty()) {
            LOGGER.log(Level.FINE, "No pending lessons found.");
            return new java.util.ArrayList<>();
        }

        LOGGER.log(Level.INFO, "Found {0} pending lessons. Starting enrichment...", new Object[]{lessons.size()});

        java.util.List<com.aicourse.dto.GenerationLog> logs = new java.util.ArrayList<>();

        for (Lesson lesson : lessons) {
            Long courseId = lesson.getModule().getCourse().getId();
            Long moduleId = lesson.getModule().getId();
            Long lessonId = lesson.getId();
            String lessonTitle = lesson.getTitle();
            String moduleTitle = lesson.getModule().getTitle();
            String courseTitle = lesson.getModule().getCourse().getTitle();

            try {
                LOGGER.log(Level.INFO, "Enriching Lesson ID: {0}...", new Object[]{lessonId});
                generateLessonContent(courseId, moduleId, lessonId, null); // this call is for background run so we don't need userid so far this lesson generation
                
                logs.add(new com.aicourse.dto.GenerationLog(
                    lessonId, lessonTitle, moduleId, moduleTitle, courseId, courseTitle, true, null, System.currentTimeMillis()
                ));
            } catch (Exception e) {
                // IMPORTANT: don't kill scheduler
                LOGGER.log(Level.SEVERE, "Error enriching pending lesson ID: {0}: {1}",
                        new Object[]{lessonId, e.getMessage()});
                
                logs.add(new com.aicourse.dto.GenerationLog(
                    lessonId, lessonTitle, moduleId, moduleTitle, courseId, courseTitle, false, e.getMessage(), System.currentTimeMillis()
                ));
            }
        }
        return logs;
    }

    @Override
    public Lesson getLesson(Long lessonId) throws Exception {
        LOGGER.log(Level.FINE, "Retrieving lesson by ID: {0}", new Object[]{lessonId});
        return lessonRepo.findById(lessonId)
                .orElseThrow(() -> {
                    LOGGER.log(Level.SEVERE, "Lesson not found with id: {0}", new Object[]{lessonId});
                    return new RuntimeException("Lesson not found with id: " + lessonId);
                });
    }

    public Lesson getLessonForUser(Long lessonId, Long userId) throws Exception {
        Lesson lesson = getLesson(lessonId);
        Long courseId = lesson.getModule().getCourse().getId();
        sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        return lesson;
    }

    @Override
    @Transactional
    public List<Lesson> batchGenerateLessonsForModule(Long courseId, Long moduleId, int limit, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Batch generating up to {0} lessons for Module ID: {1}", new Object[]{limit, moduleId});
        
        // Ensure user has access
        if (userId != null) {
            sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        }

        List<Lesson> pendingLessons = lessonRepo.findPendingLessonsByModuleId(moduleId, limit);
        List<Lesson> generatedLessons = new java.util.ArrayList<>();

        if (pendingLessons.isEmpty()) {
            LOGGER.log(Level.INFO, "No pending lessons found for Module ID: {0}", new Object[]{moduleId});
            return generatedLessons;
        }

        for (Lesson lesson : pendingLessons) {
            try {
                Lesson generated = generateLessonContent(courseId, moduleId, lesson.getId(), userId);
                generatedLessons.add(generated);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in batch generating lesson ID: {0}: {1}", 
                    new Object[]{lesson.getId(), e.getMessage()});
                // We continue generating others even if one fails
            }
        }

        return generatedLessons;
    }
}
