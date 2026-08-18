package com.sharing.service.impl;

import com.aicourse.model.Course;
import com.aicourse.model.Lesson;
import com.aicourse.model.Module;
import com.aicourse.repo.CourseRepo;
import com.aicourse.repo.LessonRepo;
import com.aicourse.repo.ModuleRepo;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.sharing.dto.*;
import com.sharing.model.*;
import com.sharing.repo.*;
import com.sharing.service.LessonProgressService;
import com.sharing.service.SharedCourseAccessGuard;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class LessonProgressServiceImpl implements LessonProgressService {

    private static final Logger LOGGER = Logger.getLogger(LessonProgressServiceImpl.class.getName());
    private static final long DEFAULT_MIN_LESSON_SECONDS = 60L;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private LessonProgressRepo lessonProgressRepo;

    @Autowired
    private CourseEnrollmentRepo courseEnrollmentRepo;

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private LessonRepo lessonRepo;

    @Autowired
    private ModuleRepo moduleRepo;

    @Autowired
    private LessonSessionRepo lessonSessionRepo;

    @Autowired
    private LessonQuizAttemptRepo lessonQuizAttemptRepo;

    @Autowired
    private CourseProgressPolicyRepo courseProgressPolicyRepo;

    @Autowired
    private SharedCourseAccessGuard sharedCourseAccessGuard;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CourseShareLinkRepo courseShareLinkRepo;

    @Autowired
    private CourseShareLinkAllowedUserRepo allowedUserRepo;

    @Override
    @Transactional
    public void markLessonComplete(Long lessonId, Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Marking lesson {0} complete for user {1}", new Object[]{lessonId, userId});

        try {
            sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);

            // Ensure enrollment exists; creators are auto-enrolled on first progress interaction.
            getOrCreateEnrollment(courseId, userId);

            Optional<LessonProgress> existingProgress = lessonProgressRepo.findByLessonIdAndUserId(lessonId, userId);
            LessonProgress lessonProgress = existingProgress.orElseGet(() -> new LessonProgress(lessonId, userId, courseId));

            OffsetDateTime now = OffsetDateTime.now();
            long addedSeconds = closeOpenSessionIfNeeded(lessonId, userId, now);
            long totalTimeSeconds = safeLong(lessonProgress.getTotalTimeSeconds()) + addedSeconds;

            lessonProgress.setTotalTimeSeconds(totalTimeSeconds);
            lessonProgress.setIsCompleted(true);
            lessonProgress.setCompletedAt(now);
            lessonProgress.setProgressPercentage(100.0);
            lessonProgress.setUpdatedAt(now);
            lessonProgress.setLastActivityAt(now);

            long completionDurationSeconds = 0L;
            if (lessonProgress.getStartedAt() != null) {
                completionDurationSeconds = Math.max(0L, Duration.between(lessonProgress.getStartedAt(), now).getSeconds());
            }
            lessonProgress.setCompletionDurationSeconds(completionDurationSeconds);

            long minLessonSeconds = resolveMinLessonSeconds(courseId);
            boolean flagged = totalTimeSeconds < minLessonSeconds || completionDurationSeconds < minLessonSeconds;
            lessonProgress.setCompletionFlagged(flagged);
            lessonProgress.setCompletionFlagReason(flagged ? "COMPLETED_TOO_FAST" : null);

            lessonProgressRepo.save(lessonProgress);

            updateEnrollmentProgress(courseId, userId);
            LOGGER.log(Level.INFO, "Lesson marked as complete successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking lesson complete: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void markLessonIncomplete(Long lessonId, Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Marking lesson {0} incomplete for user {1}", new Object[]{lessonId, userId});

        try {
            sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);

            Optional<LessonProgress> lessonProgress = lessonProgressRepo.findByLessonIdAndUserId(lessonId, userId);

            if (lessonProgress.isPresent()) {
                LessonProgress progress = lessonProgress.get();
                progress.setIsCompleted(false);
                progress.setCompletedAt(null);
                progress.setProgressPercentage(0.0);
                progress.setUpdatedAt(OffsetDateTime.now());
                progress.setLastActivityAt(OffsetDateTime.now());
                lessonProgressRepo.save(progress);

                updateEnrollmentProgress(courseId, userId);
                LOGGER.log(Level.INFO, "Lesson marked as incomplete successfully");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking lesson incomplete: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void startLessonSession(Long lessonId, Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Starting lesson session for lesson {0} user {1}", new Object[]{lessonId, userId});

        sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        getOrCreateEnrollment(courseId, userId);

        Optional<LessonSession> openSession = lessonSessionRepo
                .findTopByLessonIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(lessonId, userId);
        OffsetDateTime now = OffsetDateTime.now();

        LessonProgress progress = lessonProgressRepo.findByLessonIdAndUserId(lessonId, userId)
                .orElseGet(() -> new LessonProgress(lessonId, userId, courseId));

        if (openSession.isPresent()) {
            progress.setLastActivityAt(now);
            progress.setUpdatedAt(now);
            lessonProgressRepo.save(progress);
            return;
        }

        LessonSession session = new LessonSession(lessonId, courseId, userId, now);
        lessonSessionRepo.save(session);

        progress.setLastSessionStartedAt(now);
        progress.setLastActivityAt(now);
        progress.setUpdatedAt(now);
        lessonProgressRepo.save(progress);
    }

    @Override
    @Transactional
    public void stopLessonSession(Long lessonId, Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Stopping lesson session for lesson {0} user {1}", new Object[]{lessonId, userId});

        sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);

        OffsetDateTime now = OffsetDateTime.now();
        long addedSeconds = closeOpenSessionIfNeeded(lessonId, userId, now);

        if (addedSeconds <= 0L) {
            return;
        }

        LessonProgress progress = lessonProgressRepo.findByLessonIdAndUserId(lessonId, userId)
                .orElseGet(() -> new LessonProgress(lessonId, userId, courseId));

        progress.setTotalTimeSeconds(safeLong(progress.getTotalTimeSeconds()) + addedSeconds);
        progress.setLastActivityAt(now);
        progress.setUpdatedAt(now);
        lessonProgressRepo.save(progress);
    }

    @Override
    @Transactional
    public void recordQuizAttempt(Long lessonId, Long courseId, Long userId, int quizIndex, boolean correct) throws Exception {
        LOGGER.log(Level.INFO, "Recording quiz attempt for lesson {0} user {1}", new Object[]{lessonId, userId});

        sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        getOrCreateEnrollment(courseId, userId);

        if (quizIndex < 0) {
            throw new IllegalArgumentException("quizIndex must be >= 0");
        }

        Integer maxAttempt = lessonQuizAttemptRepo.findMaxAttemptNumber(lessonId, userId, quizIndex);
        int nextAttempt = (maxAttempt == null ? 0 : maxAttempt) + 1;

        LessonQuizAttempt attempt = new LessonQuizAttempt(lessonId, courseId, userId, quizIndex, nextAttempt, correct);
        lessonQuizAttemptRepo.save(attempt);

        LessonProgress progress = lessonProgressRepo.findByLessonIdAndUserId(lessonId, userId)
                .orElseGet(() -> new LessonProgress(lessonId, userId, courseId));
        progress.setLastActivityAt(OffsetDateTime.now());
        progress.setUpdatedAt(OffsetDateTime.now());
        lessonProgressRepo.save(progress);
    }

    @Override
    public SharedCourseUsageResponse getSharedCourseUsage(Long courseId, Long childUserId, Long creatorId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching shared course usage for course {0} child {1}", new Object[]{courseId, childUserId});

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        if (!course.getCreator().equals(creatorId)) {
            throw new IllegalArgumentException("User is not authorized to view this course usage");
        }

        courseEnrollmentRepo.findByCourseIdAndUserId(courseId, childUserId)
                .orElseThrow(() -> new IllegalArgumentException("User is not enrolled in this course"));

        List<Module> modules = moduleRepo.findByCourse_Id(courseId);
        List<LessonProgress> progressList = lessonProgressRepo.findByUserIdAndCourseId(childUserId, courseId);
        Map<Long, LessonProgress> progressByLesson = progressList.stream()
                .collect(Collectors.toMap(LessonProgress::getLessonId, lp -> lp, (a, b) -> a));

        List<LessonQuizAttempt> attempts = lessonQuizAttemptRepo.findByCourseIdAndUserId(courseId, childUserId);
        Map<Long, List<LessonQuizAttempt>> attemptsByLesson = attempts.stream()
                .collect(Collectors.groupingBy(LessonQuizAttempt::getLessonId));

        List<ModuleUsageResponse> moduleResponses = new ArrayList<>();
        List<LessonUsageResponse> lessonResponses = new ArrayList<>();
        List<LessonUsageResponse> pendingLessons = new ArrayList<>();

        int totalModules = modules.size();
        int completedModules = 0;
        int totalLessons = 0;
        int completedLessons = 0;
        long totalTimeSeconds = 0L;

        int totalQuizAttempts = 0;
        int totalQuizzesAttempted = 0;
        int firstAttemptCorrect = 0;

        for (Module module : modules) {
            List<Lesson> lessons = lessonRepo.findByModule_Id(module.getId());
            int moduleTotalLessons = lessons.size();
            int moduleCompletedLessons = 0;

            for (Lesson lesson : lessons) {
                LessonProgress progress = progressByLesson.get(lesson.getId());
                boolean completed = progress != null && Boolean.TRUE.equals(progress.getIsCompleted());

                if (completed) {
                    moduleCompletedLessons++;
                    completedLessons++;
                }

                Long timeSpent = progress != null ? safeLong(progress.getTotalTimeSeconds()) : 0L;
                totalTimeSeconds += timeSpent;

                QuizStats lessonQuizStats = summarizeQuizAttempts(attemptsByLesson.get(lesson.getId()));
                totalQuizAttempts += lessonQuizStats.totalAttempts;
                totalQuizzesAttempted += lessonQuizStats.totalQuizzes;
                firstAttemptCorrect += lessonQuizStats.firstAttemptCorrect;

                LessonUsageResponse lessonResponse = new LessonUsageResponse(
                        lesson.getId(),
                        lesson.getTitle(),
                        module.getId(),
                        module.getTitle(),
                        completed,
                        progress != null ? progress.getCompletedAt() : null,
                        timeSpent,
                        progress != null ? progress.getCompletionDurationSeconds() : null,
                        progress != null ? progress.getCompletionFlagged() : null,
                        progress != null ? progress.getCompletionFlagReason() : null,
                        lessonQuizStats.totalAttempts,
                        lessonQuizStats.firstAttemptCorrect,
                        lessonQuizStats.retryCount
                );
                lessonResponses.add(lessonResponse);
                if (!completed) {
                    pendingLessons.add(lessonResponse);
                }
            }

            totalLessons += moduleTotalLessons;
            double moduleProgress = moduleTotalLessons > 0 ? (moduleCompletedLessons * 100.0 / moduleTotalLessons) : 0.0;
            if (moduleTotalLessons > 0 && moduleCompletedLessons == moduleTotalLessons) {
                completedModules++;
            }

            moduleResponses.add(new ModuleUsageResponse(
                    module.getId(),
                    module.getTitle(),
                    moduleTotalLessons,
                    moduleCompletedLessons,
                    moduleProgress
            ));
        }

        int retryCount = Math.max(0, totalQuizAttempts - totalQuizzesAttempted);
        QuizSummaryResponse quizSummary = new QuizSummaryResponse(
                totalQuizAttempts,
                totalQuizzesAttempted,
                firstAttemptCorrect,
                retryCount
        );

        return new SharedCourseUsageResponse(
                courseId,
                childUserId,
                course.getTitle(),
                totalModules,
                completedModules,
                totalLessons,
                completedLessons,
                totalTimeSeconds,
                moduleResponses,
                lessonResponses,
                pendingLessons,
                quizSummary
        );
    }

    @Override
    public CourseProgressResponse getUserCourseProgress(Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching course progress for user {0} in course {1}", new Object[]{userId, courseId});

        try {
            CourseEnrollment enrollment = getOrCreateEnrollment(courseId, userId);

            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            // ✅ CHECK: Course must be active (unless user is creator)
            if (!course.isActive() && !course.getCreator().equals(userId)) {
                LOGGER.log(Level.WARNING, "Access denied: Course {0} is deactivated", courseId);
                throw new IllegalArgumentException("This course has been deactivated and is no longer accessible");
            }

            int totalLessons = getTotalLessonsInCourse(courseId);
            int completedLessons = getCompletedLessonsCount(courseId, userId);
            double progress = totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0.0;

            SharedCourseAccessGuard.ContentLockState lockState =
                    sharedCourseAccessGuard.getContentLockState(courseId, userId);

            OffsetDateTime lastAccessedAt = lessonProgressRepo
                    .findTopByUserIdAndCourseIdOrderByLastActivityAtDesc(userId, courseId)
                    .map(LessonProgress::getLastActivityAt)
                    .orElse(null);

            return new CourseProgressResponse(
                    courseId,
                    course.getTitle(),
                    course.getDescription(),
                    progress,
                    totalLessons,
                    completedLessons,
                    enrollment.getEnrolledAt(),
                    lastAccessedAt,
                    lockState.locked(),
                    lockState.reason()
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching course progress: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Long> getCompletedLessonIds(Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching completed lesson IDs for user {0} in course {1}", new Object[]{userId, courseId});

        sharedCourseAccessGuard.assertContentAccessAllowed(courseId, userId);
        getOrCreateEnrollment(courseId, userId);

        return lessonProgressRepo.findByUserIdAndCourseId(userId, courseId).stream()
                .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                .map(LessonProgress::getLessonId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseProgressResponse> getUserAllProgress(Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching all course progress for user {0}", userId);

        try {
            List<CourseEnrollment> enrollments = courseEnrollmentRepo.findByUserIdAndStatus(userId, EnrollmentStatus.ACTIVE);

            return enrollments.stream()
                    .map(enrollment -> {
                        try {
                            Course course = courseRepo.findById(enrollment.getCourseId()).orElse(null);
                            if (course != null && course.getCreator().equals(userId)) {
                                return null; // Exclude courses created by the user from "Shared With Me"
                            }
                            return getUserCourseProgress(enrollment.getCourseId(), userId);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error fetching progress for course {0}: {1}",
                                    new Object[]{enrollment.getCourseId(), e.getMessage()});
                            return null;
                        }
                    })
                    .filter(progress -> progress != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching all user progress: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public int getCompletedLessonsCount(Long courseId, Long userId) throws Exception {
        return lessonProgressRepo.countByUserIdAndCourseIdAndIsCompletedTrue(userId, courseId);
    }

    @Override
    public int getTotalLessonsInCourse(Long courseId) throws Exception {
        return (int) lessonRepo.countByCourseId(courseId);
    }

    @Override
    public EnrollmentResponse getEnrollment(Long courseId, Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching enrollment for user {0} in course {1}", new Object[]{userId, courseId});

        try {
            CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            int moduleCount = (course.getModules() != null) ? course.getModules().size() : 0;
            int lessonCount = 0;
            if (course.getModules() != null) {
                lessonCount = course.getModules().stream()
                        .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                        .sum();
            }

            Users enrolledUser = userRepo.findById(enrollment.getUserId()).orElse(null);
            String displayName = resolveDisplayName(enrolledUser);
            String handle = resolveHandle(enrolledUser);

            return new EnrollmentResponse(
                    enrollment.getId(),
                    enrollment.getCourseId(),
                    enrollment.getUserId(),
                    enrollment.getStatus(),
                    enrollment.getEnrolledAt(),
                    enrollment.getProgressPercentage(),
                    course.getTitle(),
                    course.getDescription(),
                    enrollment.getIsRead(),
                    enrollment.getInviteStatus(),
                    enrollment.getInvitedBy(),
                    null, // invitedByName
                    moduleCount,
                    lessonCount,
                    displayName,
                    handle
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching enrollment: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateEnrollmentStatus(Long courseId, Long userId, EnrollmentStatus status) throws Exception {
        LOGGER.log(Level.INFO, "Updating enrollment status for user {0} to {1}", new Object[]{userId, status});

        try {
            CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

            enrollment.setStatus(status);
            courseEnrollmentRepo.save(enrollment);
            LOGGER.log(Level.INFO, "Enrollment status updated successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating enrollment status: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<EnrollmentResponse> getCourseEnrollments(Long courseId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching all enrollments for course {0}", courseId);

        try {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            List<CourseEnrollment> enrollments = courseEnrollmentRepo.findByCourseId(courseId);

            return enrollments.stream()
                    .map(enrollment -> {
                        int mCount = (course.getModules() != null) ? course.getModules().size() : 0;
                        int lCount = 0;
                        if (course.getModules() != null) {
                            lCount = course.getModules().stream()
                                    .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                                    .sum();
                        }
                        Users enrolledUser = userRepo.findById(enrollment.getUserId()).orElse(null);
                        String displayName = resolveDisplayName(enrolledUser);
                        String handle = resolveHandle(enrolledUser);
                        return new EnrollmentResponse(
                                enrollment.getId(),
                                enrollment.getCourseId(),
                                enrollment.getUserId(),
                                enrollment.getStatus(),
                                enrollment.getEnrolledAt(),
                                enrollment.getProgressPercentage(),
                                course.getTitle(),
                                course.getDescription(),
                                enrollment.getIsRead(),
                                enrollment.getInviteStatus(),
                                enrollment.getInvitedBy(),
                                null,
                                mCount,
                                lCount,
                                displayName,
                                handle
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching course enrollments: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public PagedResponse<EnrollmentResponse> getCourseEnrollmentsPaged(Long courseId, int page, int size) throws Exception {
        LOGGER.log(Level.INFO, "Fetching paged enrollments for course {0}", courseId);

        try {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            int resolvedSize = clampPageSize(size);
            int resolvedPage = Math.max(0, page);
            int offset = resolvedPage * resolvedSize;

            List<CourseEnrollment> enrollments = courseEnrollmentRepo.findByCourseIdPaged(courseId, resolvedSize, offset);
            int totalElements = courseEnrollmentRepo.countByCourseId(courseId);
            int totalPages = resolvedSize == 0 ? 0 : (int) Math.ceil(totalElements / (double) resolvedSize);

            List<EnrollmentResponse> items = enrollments.stream()
                    .map(enrollment -> {
                        int mCount = (course.getModules() != null) ? course.getModules().size() : 0;
                        int lCount = 0;
                        if (course.getModules() != null) {
                            lCount = course.getModules().stream()
                                    .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                                    .sum();
                        }
                        Users enrolledUser = userRepo.findById(enrollment.getUserId()).orElse(null);
                        String displayName = resolveDisplayName(enrolledUser);
                        String handle = resolveHandle(enrolledUser);
                        return new EnrollmentResponse(
                                enrollment.getId(),
                                enrollment.getCourseId(),
                                enrollment.getUserId(),
                                enrollment.getStatus(),
                                enrollment.getEnrolledAt(),
                                enrollment.getProgressPercentage(),
                                course.getTitle(),
                                course.getDescription(),
                                enrollment.getIsRead(),
                                enrollment.getInviteStatus(),
                                enrollment.getInvitedBy(),
                                null,
                                mCount,
                                lCount,
                                displayName,
                                handle
                        );
                    })
                    .collect(Collectors.toList());

            return new PagedResponse<>(
                    items,
                    resolvedPage,
                    resolvedSize,
                    totalElements,
                    totalPages
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching paged enrollments: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<CourseLeaderboardEntry> getCourseLeaderboard(Long courseId, Long requestingUserId) throws Exception {
        LOGGER.log(Level.INFO, "Building leaderboard for course {0}", courseId);

        // 1. Get course to know total lessons
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        int totalLessons = (course.getModules() == null) ? 0 :
                course.getModules().stream()
                        .mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size())
                        .sum();

        // Optimal time = totalLessons * 5 minutes (300 s) — creator-configurable in future
        long optimalTotalSeconds = Math.max(1L, (long) totalLessons * 300L);

        // 2. Get all enrollments — exclude the course creator (they own it, not a student)
        Long creatorId = course.getCreator();
        List<CourseEnrollment> enrollments = courseEnrollmentRepo.findByCourseId(courseId)
                .stream()
                .filter(e -> !e.getUserId().equals(creatorId))
                .collect(Collectors.toList());

        // 3. Load all progress & quiz attempts for the course in bulk
        List<LessonProgress> allProgress = lessonProgressRepo.findByCourseId(courseId);
        List<LessonQuizAttempt> allAttempts = lessonQuizAttemptRepo.findByCourseId(courseId);

        // Group by userId
        Map<Long, List<LessonProgress>> progressByUser = allProgress.stream()
                .collect(Collectors.groupingBy(LessonProgress::getUserId));
        Map<Long, List<LessonQuizAttempt>> attemptsByUser = allAttempts.stream()
                .collect(Collectors.groupingBy(LessonQuizAttempt::getUserId));

        // 4. Score each user
        List<CourseLeaderboardEntry> entries = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments) {
            Long uid = enrollment.getUserId();
            Users leaderboardUser = userRepo.findById(uid).orElse(null);
            String displayName = resolveDisplayName(leaderboardUser);
            String handle = resolveHandle(leaderboardUser);

            List<LessonProgress> userProgress = progressByUser.getOrDefault(uid, Collections.emptyList());
            List<LessonQuizAttempt> userAttempts = attemptsByUser.getOrDefault(uid, Collections.emptyList());

            // Lesson completion score (0–500)
            int completedLessons = (int) userProgress.stream().filter(p -> Boolean.TRUE.equals(p.getIsCompleted())).count();
            double completionRatio = totalLessons > 0 ? (double) completedLessons / totalLessons : 0.0;
            double lessonScore = completionRatio * 500.0;

            // Quiz accuracy score (0–300)
            QuizStats stats = summarizeQuizAttempts(userAttempts);
            double quizAccuracy = stats.totalQuizzes() > 0
                    ? (double) stats.firstAttemptCorrect() / stats.totalQuizzes() * 100.0
                    : 0.0;
            double quizScore = (quizAccuracy / 100.0) * 300.0;

            // Engagement time score (0–200)
            long totalTime = userProgress.stream().mapToLong(p -> p.getTotalTimeSeconds() == null ? 0 : p.getTotalTimeSeconds()).sum();
            double engagementRatio = Math.min(1.0, (double) totalTime / optimalTotalSeconds);
            double engagementScore = engagementRatio * 200.0;

            // Integrity penalty: -50 per flagged lesson
            int flaggedCount = (int) userProgress.stream().filter(p -> Boolean.TRUE.equals(p.getCompletionFlagged())).count();
            double penalty = flaggedCount * 50.0;

            double rawScore = lessonScore + quizScore + engagementScore - penalty;
            double score = Math.max(0.0, rawScore);

            // Progress % for display
            double progressPct = completionRatio * 100.0;

            // FlaggedCount: only expose to the requesting user themselves or the course creator
            boolean isCreator = requestingUserId != null && requestingUserId.equals(creatorId);
            boolean isOwn = requestingUserId != null && requestingUserId.equals(uid);
            int exposedFlagCount = (isCreator || isOwn) ? flaggedCount : 0;

            entries.add(new CourseLeaderboardEntry(
                    uid, displayName, handle, 0 /* rank assigned below */,
                    Math.round(score * 10.0) / 10.0,
                    Math.round(progressPct * 10.0) / 10.0,
                    completedLessons,
                    Math.round(quizAccuracy * 10.0) / 10.0,
                    totalTime,
                    exposedFlagCount
            ));
        }

        // 5. Sort by score desc, tiebreak by display name asc
        entries.sort(Comparator.comparingDouble(CourseLeaderboardEntry::getScore).reversed()
                .thenComparing(CourseLeaderboardEntry::getUsername));

        // 6. Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    @Override
    public PagedResponse<CourseLeaderboardEntry> getCourseLeaderboardPaged(Long courseId, Long requestingUserId, int page, int size) throws Exception {
        List<CourseLeaderboardEntry> entries = getCourseLeaderboard(courseId, requestingUserId);

        int resolvedSize = clampPageSize(size);
        int resolvedPage = Math.max(0, page);
        int fromIndex = Math.min(resolvedPage * resolvedSize, entries.size());
        int toIndex = Math.min(fromIndex + resolvedSize, entries.size());

        List<CourseLeaderboardEntry> pageItems = entries.subList(fromIndex, toIndex);
        int totalPages = resolvedSize == 0 ? 0 : (int) Math.ceil(entries.size() / (double) resolvedSize);

        return new PagedResponse<>(pageItems, resolvedPage, resolvedSize, entries.size(), totalPages);
    }

    @Override
    @Transactional
    public EnrollmentResponse enrollUserInCourse(Long courseId, Long userId, Long shareLinkId) throws Exception {
        LOGGER.log(Level.INFO, "Enrolling user {0} in course {1}", new Object[]{userId, courseId});

        try {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            if (!course.isActive() && !course.getCreator().equals(userId)) {
                throw new IllegalArgumentException("This course has been deactivated and is no longer available");
            }

            CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

            if (!Objects.equals(shareLink.getCourseId(), courseId)) {
                throw new IllegalArgumentException("Share link does not match the course");
            }

            if (!shareLink.canEnroll()) {
                throw new IllegalArgumentException("Share link is no longer valid");
            }

            if (ShareLinkType.PRIVATE.equals(shareLink.getLinkType())) {
                boolean allowed = allowedUserRepo.existsByShareLinkIdAndUserId(shareLink.getId(), userId);
                if (!allowed) {
                    throw new IllegalArgumentException("You are not allowed to access this private share link");
                }
            }

            incrementShareLinkEnrollments(shareLink);

            Optional<CourseEnrollment> existing = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId);
            CourseEnrollment enrollment;

            if (existing.isPresent()) {
                enrollment = existing.get();
                if (EnrollmentStatus.ACTIVE.equals(enrollment.getStatus())) {
                    return buildEnrollmentResponse(course, enrollment);
                }

                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollment.setInviteStatus("ACCEPTED");
                enrollment.setInviteType("LINK");
                enrollment.setShareLinkId(shareLinkId);
                enrollment = courseEnrollmentRepo.save(enrollment);

                return buildEnrollmentResponse(course, enrollment);
            }

            enrollment = new CourseEnrollment(courseId, userId, shareLinkId);
            enrollment = courseEnrollmentRepo.save(enrollment);

            return buildEnrollmentResponse(course, enrollment);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error enrolling user: {0}", e.getMessage());
            throw e;
        }
    }

    // --- Helper methods ---
    private CourseEnrollment getOrCreateEnrollment(Long courseId, Long userId) {
        Optional<CourseEnrollment> existing = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // Allow the course creator to track own progress without manual self-enrollment.
        if (course.getCreator().equals(userId)) {
            CourseEnrollment creatorEnrollment = new CourseEnrollment(courseId, userId, null);
            return courseEnrollmentRepo.save(creatorEnrollment);
        }

        throw new IllegalArgumentException("User is not enrolled in this course");
    }

    private void updateEnrollmentProgress(Long courseId, Long userId) throws Exception {
        CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        int totalLessons = getTotalLessonsInCourse(courseId);
        int completedLessons = getCompletedLessonsCount(courseId, userId);
        double progress = totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0.0;

        enrollment.setProgressPercentage(progress);
        courseEnrollmentRepo.save(enrollment);
    }

    private long closeOpenSessionIfNeeded(Long lessonId, Long userId, OffsetDateTime endTime) {
        Optional<LessonSession> openSession = lessonSessionRepo
                .findTopByLessonIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(lessonId, userId);
        if (openSession.isEmpty()) {
            return 0L;
        }
        LessonSession session = openSession.get();
        long durationSeconds = session.close(endTime);
        lessonSessionRepo.save(session);
        return durationSeconds;
    }

    private long resolveMinLessonSeconds(Long courseId) {
        CourseProgressPolicy policy = courseProgressPolicyRepo.findByCourseId(courseId)
                .orElse(null);
        if (policy == null || policy.getMinLessonSeconds() == null) {
            return DEFAULT_MIN_LESSON_SECONDS;
        }
        return Math.max(0L, policy.getMinLessonSeconds());
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private QuizStats summarizeQuizAttempts(List<LessonQuizAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return new QuizStats(0, 0, 0, 0);
        }

        Map<Integer, List<LessonQuizAttempt>> byQuiz = attempts.stream()
                .collect(Collectors.groupingBy(LessonQuizAttempt::getQuizIndex));

        int totalAttempts = attempts.size();
        int totalQuizzes = byQuiz.size();
        int firstAttemptCorrect = 0;

        for (Map.Entry<Integer, List<LessonQuizAttempt>> entry : byQuiz.entrySet()) {
            LessonQuizAttempt firstAttempt = entry.getValue().stream()
                    .min((a, b) -> Integer.compare(a.getAttemptNumber(), b.getAttemptNumber()))
                    .orElse(null);
            if (firstAttempt != null && Boolean.TRUE.equals(firstAttempt.getCorrect())) {
                firstAttemptCorrect++;
            }
        }

        int retryCount = Math.max(0, totalAttempts - totalQuizzes);
        return new QuizStats(totalAttempts, totalQuizzes, firstAttemptCorrect, retryCount);
    }

    private EnrollmentResponse buildEnrollmentResponse(Course course, CourseEnrollment enrollment) {
        int moduleCount = (course.getModules() != null) ? course.getModules().size() : 0;
        int lessonCount = 0;
        if (course.getModules() != null) {
            lessonCount = course.getModules().stream()
                    .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                    .sum();
        }

        Users enrolledUser = userRepo.findById(enrollment.getUserId()).orElse(null);
        String displayName = resolveDisplayName(enrolledUser);
        String handle = resolveHandle(enrolledUser);

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getUserId(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getProgressPercentage(),
                course.getTitle(),
                course.getDescription(),
                enrollment.getIsRead(),
                enrollment.getInviteStatus(),
                enrollment.getInvitedBy(),
                null,
                moduleCount,
                lessonCount,
                displayName,
                handle
        );
    }

    private void incrementShareLinkEnrollments(CourseShareLink shareLink) {
        Integer current = shareLink.getCurrentEnrollments();
        shareLink.setCurrentEnrollments((current == null ? 0 : current) + 1);
        courseShareLinkRepo.save(shareLink);
    }

    private int clampPageSize(int size) {
        int resolved = size <= 0 ? 20 : size;
        return Math.min(resolved, MAX_PAGE_SIZE);
    }

    private String resolveDisplayName(Users user) {
        if (user == null) {
            return "Unknown";
        }
        String name = user.getDisplayName();
        if (name == null || name.isBlank()) {
            return user.getUsername();
        }
        return name;
    }

    private String resolveHandle(Users user) {
        return user == null ? "" : user.getUsername();
    }

    private record QuizStats(int totalAttempts, int totalQuizzes, int firstAttemptCorrect, int retryCount) {
    }
}
