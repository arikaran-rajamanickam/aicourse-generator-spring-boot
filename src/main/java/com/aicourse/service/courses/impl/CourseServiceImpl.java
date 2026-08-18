package com.aicourse.service.courses.impl;

import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.service.AiDynamicGateway;
import com.aicourse.dto.CourseBuilderRequest;
import com.aicourse.model.Course;
import com.aicourse.model.Lesson;
import com.aicourse.model.Module;
import com.aicourse.repo.CourseRepo;
import com.aicourse.repo.LessonRepo;
import com.aicourse.repo.ModuleRepo;
import com.aicourse.service.courses.CourseOutlinePromptBuilder;
import com.aicourse.service.courses.CourseService;
import com.aicourse.utils.id.SnowflakeIdGenerator;
import com.aicourse.utils.json.JsonParserUtil;
import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.fasterxml.jackson.databind.JsonNode;
import com.features.Feature;
import com.features.FeatureGuard;
import com.leaderboard.model.impl.UserStatsService;
import com.sharing.repo.CourseEnrollmentRepo;
import com.sharing.repo.CourseShareLinkRepo;
import com.sharing.service.SharedCourseAccessGuard;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger LOGGER = Logger.getLogger(CourseServiceImpl.class.getName());

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private ModuleRepo moduleRepo;

    @Autowired
    private AiDynamicGateway aiDynamicGateway;

    @Autowired
    private FeatureGuard featureGuard;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private CourseShareLinkRepo courseShareLinkRepo;

    @Autowired
    private CourseEnrollmentRepo courseEnrollmentRepo;

    @Autowired
    private SharedCourseAccessGuard sharedCourseAccessGuard;

    @Autowired
    private LessonRepo lessonRepo;

    @Override
    @Transactional
    public Course saveBuiltCourse(CourseBuilderRequest payload, Authentication auth) throws Exception {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        validateCourseCreationLimit(userId, principal.getUser().getRoles());

        Course course = new Course();
        course.setId(SnowflakeIdGenerator.generateId());
        course.setCreator(userId);

        populateCourseBasicInfo(course, payload);
        populateCourseModulesAndLessons(course, payload);

        Course savedCourse = courseRepo.save(course);
        userStatsService.incrementTotalCoursesCreated(userId);
        LOGGER.log(Level.INFO, "Custom Course built and saved successfully with ID: {0}", savedCourse.getId());
        return savedCourse;
    }

    @Override
    public JsonNode generateCourseOutlineOnly(Map<String, String> payload, Authentication auth) throws Exception {
        String title = payload.get("title");
        String difficulty = payload.getOrDefault("difficulty", "Beginner");
        String duration = payload.getOrDefault("duration", "2 Hours");

        LOGGER.log(Level.INFO, "Generating course OUTLINE ''{0}'' (Difficulty: {1}, Duration: {2})",
                new Object[]{title, difficulty, duration});

        String prompt = buildOutlinePrompt(title, difficulty, duration);

        try {
            LOGGER.log(Level.FINE, "Sending prompt to AI for outline generation: {0}", new Object[]{title});
            String response = aiDynamicGateway.getResponse(AiWorkload.COURSE_GENERATION, prompt);
            LOGGER.log(Level.FINE, "Received response from AI");

            String cleanJson = JsonParserUtil.extractRawJson(response);
            return JsonParserUtil.parseStringToJsonObject(cleanJson);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate course outline ''{0}'': {1}", new Object[]{title, e.getMessage()});
            throw e;
        }
    }

    @Override
    @Transactional
    public Course generateCourse(Map<String, String> payload, Authentication auth) throws Exception {

        String title = payload.get("title");
        String difficulty = payload.getOrDefault("difficulty", "Beginner");
        String duration = payload.getOrDefault("duration", "2 Hours");
        String creator = auth.getName();
        LOGGER.log(Level.INFO, "Generating course ''{0}'' for user ''{1}'' (Difficulty: {2}, Duration: {3})",
                new Object[]{title, creator, difficulty, duration});
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Users curUser = principal.getUser();
        Long userId = curUser.getId();

//      int existingCount = courseRepo.countByCreator(userId);
        int lifetimeCount = userStatsService.getTotalCoursesCreated(userId);
        featureGuard.requireWithinLimit(Feature.COURSE_CREATE, curUser.getRoles(), lifetimeCount);

        Course course = new Course();
        course.setId(SnowflakeIdGenerator.generateId());
        course.setTitle(title);

        course.setCreator(userId);

        String prompt = new CourseOutlinePromptBuilder()
                .title(title)
                .difficulty(difficulty)
                .duration(duration)
                .build();

        try {
            LOGGER.log(Level.FINE, "Sending prompt to AI for course generation: {0}", new Object[]{title});
            String response = aiDynamicGateway.getResponse(AiWorkload.COURSE_GENERATION, prompt);
            LOGGER.log(Level.FINE, "Received response from AI");

            String cleanJson = JsonParserUtil.extractRawJson(response);
            JsonNode courseJson = JsonParserUtil.parseStringToJsonObject(cleanJson);

            course.setDescription(
                    courseJson.has("description")
                            ? courseJson.get("description").asText()
                            : "Generated course for " + title);

            if (courseJson.has("overview")) {
                course.setOverview(courseJson.get("overview"));
            }
            if (courseJson.has("capstoneProject")) {
                course.setCapstoneProject(courseJson.get("capstoneProject"));
            }

            List<Module> modules = new ArrayList<>();

            JsonNode modulesNode = courseJson.get("modules");
            if (modulesNode == null || !modulesNode.isArray()) {
                LOGGER.log(Level.SEVERE, "Invalid AI response: 'modules' missing or not an array");
                throw new IllegalArgumentException("Invalid AI response: modules missing");
            }

            for (JsonNode moduleNode : modulesNode) {
                Module module = new Module();
                module.setId(SnowflakeIdGenerator.generateId());
                module.setTitle(moduleNode.has("title") ? moduleNode.get("title").asText() : "Untitled Module");
                module.setDescription(moduleNode.has("description") ? moduleNode.get("description").asText() : null);
                module.setModuleLevel(moduleNode.has("moduleLevel") ? moduleNode.get("moduleLevel").asText() : null);
                module.setEstimatedMinutes(moduleNode.has("estimatedMinutes") ? moduleNode.get("estimatedMinutes").asInt() : null);
                module.setLearningObjectives(moduleNode.has("learningObjectives") ? moduleNode.get("learningObjectives") : null);
                module.setCourse(course);

                List<Lesson> lessons = new ArrayList<>();
                JsonNode lessonsNode = moduleNode.get("lessons");
                if (lessonsNode != null && lessonsNode.isArray()) {
                    for (int i = 0; i < lessonsNode.size(); i++) {
                        JsonNode lessonNode = lessonsNode.get(i);
                        Lesson lesson = new Lesson();
                        lesson.setId(SnowflakeIdGenerator.generateId());

                        String lessonTitle = lessonNode.isObject() && lessonNode.has("title")
                                ? lessonNode.get("title").asText()
                                : lessonNode.asText();

                        lesson.setTitle(lessonTitle);
                        lesson.setOrder(i);

                        // Always start with empty content to trigger the rich LessonPromptBuilder flow
                        lesson.setContent(JsonParserUtil.parseStringToJsonObject("[]"));


                        if (lessonNode.isObject() && lessonNode.has("estimatedMinutes")) {
                            lesson.setEstimatedMinutes(lessonNode.get("estimatedMinutes").asInt());
                        }

                        lesson.setModule(module);

                        // Parse sub-lessons if present
                        if (lessonNode.isObject() && lessonNode.has("subLessons") && lessonNode.get("subLessons").isArray()) {
                            List<Lesson> subLessonsList = new ArrayList<>();
                            JsonNode subNodes = lessonNode.get("subLessons");
                            for (int j = 0; j < subNodes.size(); j++) {
                                JsonNode subNode = subNodes.get(j);
                                Lesson subLesson = new Lesson();
                                subLesson.setId(SnowflakeIdGenerator.generateId());

                                String subTitle = subNode.isObject() && subNode.has("title")
                                        ? subNode.get("title").asText()
                                        : subNode.asText();

                                subLesson.setTitle(subTitle);
                                subLesson.setOrder(j);
                                subLesson.setContent(JsonParserUtil.parseStringToJsonObject("[]"));

                                if (subNode.isObject() && subNode.has("estimatedMinutes")) {
                                    subLesson.setEstimatedMinutes(subNode.get("estimatedMinutes").asInt());
                                }

                                subLesson.setModule(module);
                                subLesson.setParentLesson(lesson);
                                subLessonsList.add(subLesson);
                            }
                            lesson.setSubLessons(subLessonsList);
                        }

                        lessons.add(lesson);
                    }
                }

                module.setLessons(lessons);
                modules.add(module);
            }

            course.setModules(modules);
            Course savedCourse = courseRepo.save(course);
            userStatsService.incrementTotalCoursesCreated(userId);
            LOGGER.log(Level.INFO, "Course ''{0}'' generated and saved successfully with ID: {1}",
                    new Object[]{title, savedCourse.getId()});
            return savedCourse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate course ''{0}'': {1}", new Object[]{title, e.getMessage()});
            throw e; // Re-throw to be handled by controller
        }
    }

    @Override
    @Transactional
    public List<Course> getCoursesByCreator(Long creator) throws Exception {
        LOGGER.log(Level.FINE, "Retrieving courses for creator: {0}", new Object[]{creator});
        List<Course> courses = courseRepo.findByCreator(creator);
        for (Course course : courses) {
            if (course.getModules() != null) {
                course.getModules().size();
            }
        }
        return courses;
    }

    @Override
    @Transactional
    public List<Course> getCoursesSharedByCreator(Long creator) throws Exception {
        LOGGER.log(Level.FINE, "Retrieving courses shared by creator: {0}", new Object[]{creator});
        Set<Long> courseIds = new LinkedHashSet<>(courseShareLinkRepo.findDistinctCourseIdsByCreatedBy(creator));
        courseIds.addAll(courseEnrollmentRepo.findDistinctCourseIdsByInvitedByAndInviteType(creator, "DIRECT"));
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<Course> courses = courseRepo.findAllById(courseIds);
        for (Course course : courses) {
            if (course.getModules() != null) {
                course.getModules().size();
            }
        }
        return courses;
    }

    @Override
    @Transactional
    public Course getCourseById(Long id, Long requesterId) throws Exception {
        LOGGER.log(Level.FINE, "Retrieving course by ID: {0}", new Object[]{id});
        Course course = sharedCourseAccessGuard.assertCourseShellAccess(id, requesterId);
        if (course.getModules() != null) {
            course.getModules().forEach(module -> {
                if (module.getLessons() != null) {
                    module.getLessons().forEach(lesson -> {
                        if (lesson.getSubLessons() != null) {
                            lesson.getSubLessons().size();
                        }
                    });
                }
            });
        }
        LOGGER.log(Level.FINE, "Course access granted for user {0} and course {1}",
                new Object[]{requesterId, id});
        return course;
    }

    @Override
    public List<Module> getModulesByCourseName(String courseName) throws Exception {
        LOGGER.log(Level.FINE, "Retrieving modules for course name: {0}", new Object[]{courseName});
        return moduleRepo.findByCourse_Title(courseName);
    }

    @Override
    public void deleteCourse(Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Attempting to delete course with ID: {0}", new Object[]{courseId});
        if (!courseRepo.existsById(courseId)) {
            LOGGER.log(Level.SEVERE, "Cannot delete course. Course not found with ID: {0}", new Object[]{courseId});
            throw new IllegalArgumentException("Course not found with id: " + courseId);
        }
        courseRepo.deleteById(courseId);
        LOGGER.log(Level.INFO, "Course with ID: {0} deleted successfully", new Object[]{courseId});
    }

    @Override
    public void updateCourse(Long courseID, Course courseDO) throws Exception {
        LOGGER.log(Level.INFO, "Updating course with ID: {0}", new Object[]{courseID});
        Course course = courseRepo.findById(courseID).orElseThrow(() -> {
            LOGGER.log(Level.SEVERE, "Cannot update course. Course not found with ID: {0}", new Object[]{courseID});
            return new IllegalArgumentException("Course not found with id: " + courseID);
        });

        if (courseDO.getTitle() != null) {
            course.setTitle(courseDO.getTitle());
        }
        if (courseDO.getCreator() != null) {
            course.setCreator(courseDO.getCreator());
        }
        if (courseDO.getDescription() != null) {
            course.setDescription(courseDO.getDescription());
        }
        if (courseDO.getModules() != null) {
            course.setModules(courseDO.getModules());
        }
        courseRepo.save(course);
        LOGGER.log(Level.INFO, "Course with ID: {0} updated successfully", new Object[]{courseID});
    }

    @Override
    @Transactional
    public void deactivateCourse(Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Deactivating course with ID: {0}", new Object[]{courseId});
        Course course = courseRepo.findById(courseId).orElseThrow(() -> {
            LOGGER.log(Level.SEVERE, "Cannot deactivate course. Course not found with ID: {0}", new Object[]{courseId});
            return new IllegalArgumentException("Course not found with id: " + courseId);
        });

        course.setActive(false);
        courseRepo.save(course);

        LOGGER.log(Level.INFO, "Course with ID: {0} deactivated successfully", new Object[]{courseId});
    }

    @Override
    @Transactional
    public void activateCourse(Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Activating course with ID: {0}", new Object[]{courseId});
        Course course = courseRepo.findById(courseId).orElseThrow(() -> {
            LOGGER.log(Level.SEVERE, "Cannot activate course. Course not found with ID: {0}", new Object[]{courseId});
            return new IllegalArgumentException("Course not found with id: " + courseId);
        });

        course.setActive(true);
        courseRepo.save(course);

        LOGGER.log(Level.INFO, "Course with ID: {0} activated successfully", new Object[]{courseId});
    }

    @Override
    @Transactional
    public Module addModule(Long courseId, String title) throws Exception {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        Module module = new Module();
        module.setId(SnowflakeIdGenerator.generateId());
        module.setTitle(title);
        module.setCourse(course);
        module.setOrder(course.getModules().size());
        return moduleRepo.save(module);
    }

    @Override
    @Transactional
    public void renameModule(Long moduleId, String title) throws Exception {
        Module module = moduleRepo.findById(moduleId).orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
        module.setTitle(title);
        moduleRepo.save(module);
    }

    @Override
    @Transactional
    public void deleteModule(Long moduleId) throws Exception {
        moduleRepo.deleteById(moduleId);
    }

    @Override
    @Transactional
    public Lesson addLesson(Long moduleId, String title) throws Exception {
        Module module = moduleRepo.findById(moduleId).orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
        Lesson lesson = new Lesson();
        lesson.setId(SnowflakeIdGenerator.generateId());
        lesson.setTitle(title);
        lesson.setModule(module);
        lesson.setOrder(module.getLessons().size());
        lesson.setContent(JsonParserUtil.parseStringToJsonObject("[]"));
        lesson.setContentMd("");
        return lessonRepo.save(lesson);
    }

    @Override
    @Transactional
    public void renameLesson(Long lessonId, String title) throws Exception {
        Lesson lesson = lessonRepo.findById(lessonId).orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
        lesson.setTitle(title);
        lessonRepo.save(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId) throws Exception {
        lessonRepo.deleteById(lessonId);
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    /**
     * Validates course creation limit for the user based on their role.
     *
     * @param userId   User ID
     * @param userRole User role
     * @throws Exception if user exceeds course creation limit
     */
    private void validateCourseCreationLimit(Long userId, UserRole userRole) throws Exception {
        int lifetimeCount = userStatsService.getTotalCoursesCreated(userId);
        featureGuard.requireWithinLimit(Feature.COURSE_CREATE, userRole, lifetimeCount);
    }

    /**
     * Populates basic course information from the request payload.
     *
     * @param course  Course object to populate
     * @param payload Course builder request payload
     */
    private void populateCourseBasicInfo(Course course, CourseBuilderRequest payload) {
        course.setTitle(payload.getTitle() != null && !payload.getTitle().isEmpty() ? payload.getTitle() : "Untitled");
        course.setDescription(payload.getDescription());
        course.setCategory(payload.getCategory());
        course.setTagsJson(payload.getTags());
        course.setDifficulty(payload.getDifficulty());
        course.setFinalExam(payload.getFinalExam());
        course.setThumbnailUrl(payload.getThumbnailUrl());

        if (payload.getEstimatedDuration() != null) {
            course.setEstimatedDurationValue(payload.getEstimatedDuration().getValue());
            course.setEstimatedDurationUnit(payload.getEstimatedDuration().getUnit());
        }

        if (payload.getSettings() != null) {
            course.setVisibility(payload.getSettings().getVisibility());
            course.setEnrollmentType(payload.getSettings().getEnrollmentType());
        }
    }

    /**
     * Populates modules and lessons for a course from the request payload.
     *
     * @param course  Course object to populate
     * @param payload Course builder request payload
     */
    private void populateCourseModulesAndLessons(Course course, CourseBuilderRequest payload) throws Exception {
        List<Module> modules = new ArrayList<>();

        if (payload.getModules() != null) {
            for (int i = 0; i < payload.getModules().size(); i++) {
                CourseBuilderRequest.ModuleRequest modReq = payload.getModules().get(i);
                Module module = buildModule(modReq, i, course);
                modules.add(module);
            }
        }

        course.setModules(modules);
    }

    /**
     * Builds a module with its lessons from the module request.
     *
     * @param modReq Module request
     * @param index  Module index
     * @param course Parent course
     * @return Populated Module object
     */
    private Module buildModule(CourseBuilderRequest.ModuleRequest modReq, int index, Course course) throws Exception {
        Module module = new Module();
        module.setId(SnowflakeIdGenerator.generateId());
        module.setTitle(modReq.getTitle() != null ? modReq.getTitle() : "Untitled Module");
        module.setDescription(modReq.getDescription());
        module.setLearningObjectives(modReq.getLearningObjectives());
        module.setAssessment(modReq.getAssessment());
        module.setOrder(modReq.getOrder() != null ? modReq.getOrder() : index);
        module.setCourse(course);

        List<Lesson> lessons = new ArrayList<>();
        if (modReq.getLessons() != null) {
            for (int j = 0; j < modReq.getLessons().size(); j++) {
                CourseBuilderRequest.LessonRequest lessReq = modReq.getLessons().get(j);
                Lesson lesson = buildLesson(lessReq, j, module);
                lessons.add(lesson);
            }
        }

        module.setLessons(lessons);
        return module;
    }

    /**
     * Builds a lesson from the lesson request.
     *
     * @param lessReq Lesson request
     * @param index   Lesson index
     * @param module  Parent module
     * @return Populated Lesson object
     */
    private Lesson buildLesson(CourseBuilderRequest.LessonRequest lessReq, int index, Module module) throws Exception {
        Lesson lesson = new Lesson();
        lesson.setId(SnowflakeIdGenerator.generateId());
        lesson.setTitle(lessReq.getTitle() != null ? lessReq.getTitle() : "Untitled Lesson");
        lesson.setContent(lessReq.getContentBlocks() != null ? lessReq.getContentBlocks() : JsonParserUtil.parseStringToJsonObject("[]"));
        lesson.setOrder(lessReq.getOrder() != null ? lessReq.getOrder() : index);
        lesson.setModule(module);
        return lesson;
    }

    /**
     * Builds the prompt for course outline generation.
     *
     * @param title      Course title
     * @param difficulty Course difficulty level
     * @param duration   Estimated course duration
     * @return Formatted prompt string
     */
    private String buildOutlinePrompt(String title, String difficulty, String duration) {
        return """
                Create a full editable course draft about "%s".
                Difficulty: %s
                Duration: %s
                
                IMPORTANT:
                - Do NOT return outline-only data.
                - Every lesson must include substantial teaching content.
                - Keep all content practical, clear, and beginner-friendly for the specified difficulty.
                - Return ONLY raw JSON (no markdown, no explanation text).
                
                Required JSON shape:
                {
                  "title": "Course Title",
                  "description": "Course Description",
                  "modules": [
                    {
                      "title": "Module Title",
                      "description": "Module description",
                      "learningObjectives": ["objective 1", "objective 2"],
                      "lessons": [
                        {
                          "title": "Lesson Title",
                          "contentBlocks": [
                            {
                              "type": "text",
                              "content": "Detailed lesson explanation with examples, step-by-step guidance, and key takeaways."
                            },
                            {
                              "type": "text",
                              "content": "Practice tasks or mini exercises for the learner."
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                
                Constraints:
                - 3 to 6 modules.
                - 3 to 6 lessons per module.
                - At least 2 text content blocks per lesson.
                - Each text block should be useful and non-trivial (not one-liners).
                """.formatted(title, difficulty, duration);
    }
}
