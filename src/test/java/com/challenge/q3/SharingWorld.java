package com.challenge.q3;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.aicourse.repo.LessonRepo;
import com.aicourse.repo.ModuleRepo;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.sharing.dto.EnrollmentResponse;
import com.sharing.dto.ShareLinkResponse;
import com.sharing.model.CourseEnrollment;
import com.sharing.model.CourseShareLink;
import com.sharing.model.CourseShareLinkAllowedUser;
import com.sharing.model.LessonProgress;
import com.sharing.model.ShareLinkType;
import com.sharing.repo.CourseEnrollmentRepo;
import com.sharing.repo.CourseProgressPolicyRepo;
import com.sharing.repo.CourseShareLinkAllowedUserRepo;
import com.sharing.repo.CourseShareLinkRepo;
import com.sharing.repo.LessonProgressRepo;
import com.sharing.repo.LessonQuizAttemptRepo;
import com.sharing.repo.LessonSessionRepo;
import com.sharing.service.NotificationService;
import com.sharing.service.SharedCourseAccessGuard;
import com.sharing.service.impl.CourseShareServiceImpl;
import com.sharing.service.impl.LessonProgressServiceImpl;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test harness for the course-sharing flow.
 *
 * <p>The sharing services are exercised as a black box: every repository they depend on is mocked
 * and backed by an in-memory map, so a test can drive the real creator -&gt; share link -&gt;
 * learner join -&gt; lesson progress sequence with no database, no Spring context and no network.
 *
 * <p>{@link #resolve(String, Long)} performs exactly the call that {@code GET /api/join/{token}}
 * performs, and {@link #join(String, Long)} performs exactly the two calls, in the same order,
 * that {@code POST /api/join/{token}/enroll} performs.
 */
final class SharingWorld {

    static final long CREATOR_ID = 900_001L;
    static final long COURSE_ID = 500_001L;

    private final Map<Long, CourseShareLink> linksById = new LinkedHashMap<>();
    private final Map<String, CourseEnrollment> enrollmentsByCourseUser = new LinkedHashMap<>();
    private final Map<String, LessonProgress> lessonProgress = new LinkedHashMap<>();
    private final Map<Long, Users> usersById = new LinkedHashMap<>();
    private final Set<String> allowlistRows = new LinkedHashSet<>();

    private final Course course = new Course();
    private int totalLessons = 4;

    final CourseShareServiceImpl shareService = new CourseShareServiceImpl();
    final LessonProgressServiceImpl progressService = new LessonProgressServiceImpl();

    private final CourseShareLinkRepo shareLinkRepo = mock(CourseShareLinkRepo.class);
    private final CourseEnrollmentRepo enrollmentRepo = mock(CourseEnrollmentRepo.class);
    private final CourseShareLinkAllowedUserRepo allowedUserRepo = mock(CourseShareLinkAllowedUserRepo.class);
    private final CourseRepo courseRepo = mock(CourseRepo.class);
    private final UserRepo userRepo = mock(UserRepo.class);
    private final LessonRepo lessonRepo = mock(LessonRepo.class);
    private final LessonProgressRepo lessonProgressRepo = mock(LessonProgressRepo.class);
    private final SharedCourseAccessGuard accessGuard = mock(SharedCourseAccessGuard.class);

    SharingWorld() {
        course.setId(COURSE_ID);
        course.setTitle("Distributed Systems 101");
        course.setDescription("Consensus, replication and failure modes");
        course.setCreator(CREATOR_ID);
        course.setActive(true);
        addUser(CREATOR_ID, "creator");

        stubShareLinkRepo();
        stubEnrollmentRepo();
        stubAllowlistRepo();
        stubCourseAndUserRepos();
        stubProgressRepos();
        when(accessGuard.getContentLockState(anyLong(), anyLong()))
                .thenReturn(new SharedCourseAccessGuard.ContentLockState(false, null));

        inject(shareService, "courseShareLinkRepo", shareLinkRepo);
        inject(shareService, "courseRepo", courseRepo);
        inject(shareService, "courseEnrollmentRepo", enrollmentRepo);
        inject(shareService, "userRepo", userRepo);
        inject(shareService, "allowedUserRepo", allowedUserRepo);
        inject(shareService, "notificationService", mock(NotificationService.class));

        inject(progressService, "lessonProgressRepo", lessonProgressRepo);
        inject(progressService, "courseEnrollmentRepo", enrollmentRepo);
        inject(progressService, "courseRepo", courseRepo);
        inject(progressService, "lessonRepo", lessonRepo);
        inject(progressService, "moduleRepo", mock(ModuleRepo.class));
        inject(progressService, "lessonSessionRepo", mock(LessonSessionRepo.class));
        inject(progressService, "lessonQuizAttemptRepo", mock(LessonQuizAttemptRepo.class));
        inject(progressService, "courseProgressPolicyRepo", mock(CourseProgressPolicyRepo.class));
        inject(progressService, "sharedCourseAccessGuard", accessGuard);
        inject(progressService, "userRepo", userRepo);
        inject(progressService, "courseShareLinkRepo", shareLinkRepo);
        inject(progressService, "allowedUserRepo", allowedUserRepo);
    }

    // --- fixture setup -------------------------------------------------------------------

    Users addUser(long id, String username) {
        Users user = new Users();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        usersById.put(id, user);
        return user;
    }

    /** Registers {@code count} learners with ids 1000, 1001, ... and returns their ids. */
    List<Long> addLearners(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long id = 1000L + i;
            addUser(id, "learner" + i);
            ids.add(id);
        }
        return ids;
    }

    void setTotalLessons(int lessons) {
        this.totalLessons = lessons;
    }

    void deactivateCourse() {
        course.setActive(false);
    }

    // --- creator-side operations ---------------------------------------------------------

    /** Mirrors {@code POST /api/courses/{courseId}/share/generate}. */
    ShareLinkResponse generateLink(ShareLinkType linkType, OffsetDateTime expiresAt,
                                   Integer maxEnrollments, List<String> allowedUsers) throws Exception {
        return shareService.generateShareLink(COURSE_ID, CREATOR_ID, linkType, expiresAt,
                maxEnrollments, allowedUsers);
    }

    ShareLinkResponse generatePublicLink(Integer maxEnrollments) throws Exception {
        return generateLink(ShareLinkType.PUBLIC, null, maxEnrollments, List.of());
    }

    ShareLinkResponse generatePrivateLink(Integer maxEnrollments, List<String> allowedUsernames) throws Exception {
        return generateLink(ShareLinkType.PRIVATE, null, maxEnrollments, allowedUsernames);
    }

    /** Mirrors {@code PUT /api/courses/{courseId}/share/links/{shareLinkId}/deactivate}. */
    void deactivateLink(Long shareLinkId) throws Exception {
        shareService.deactivateShareLink(shareLinkId, CREATOR_ID);
    }

    /**
     * The seat count a creator sees for one link on {@code GET /api/courses/{courseId}/share/links}
     * -- the "N of M enrolled" number on the share screen.
     */
    int seatsUsed(String token) throws Exception {
        return shareService.getCourseShareLinks(COURSE_ID, CREATOR_ID).stream()
                .filter(link -> token.equals(link.getShareToken()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("share link was not listed for its creator"))
                .getCurrentEnrollments();
    }

    // --- learner-side operations ---------------------------------------------------------

    /** Mirrors {@code GET /api/join/{token}}; {@code userId} is null for an anonymous visitor. */
    ShareLinkResponse resolve(String token, Long userId) throws Exception {
        return shareService.getShareLinkByToken(token, userId);
    }

    /** Mirrors {@code POST /api/join/{token}/enroll}. */
    EnrollmentResponse join(String token, Long userId) throws Exception {
        ShareLinkResponse link = shareService.getShareLinkByToken(token, userId);
        return progressService.enrollUserInCourse(link.getCourseId(), userId, link.getId());
    }

    /** Mirrors the enrollment-status update behind "leave course". */
    void dropOut(long userId) throws Exception {
        progressService.updateEnrollmentStatus(COURSE_ID, userId, com.sharing.model.EnrollmentStatus.DROPPED);
    }

    /** Mirrors {@code POST /api/progress/lessons/{lessonId}/complete}. */
    void completeLesson(long lessonId, long userId) throws Exception {
        progressService.markLessonComplete(lessonId, COURSE_ID, userId);
    }

    /** Mirrors {@code PUT /api/progress/lessons/{lessonId}/incomplete}. */
    void uncompleteLesson(long lessonId, long userId) throws Exception {
        progressService.markLessonIncomplete(lessonId, COURSE_ID, userId);
    }

    /** The percentage stored on the learner's enrollment record, as that enrollment reports it. */
    Double persistedProgressPercentage(long userId) throws Exception {
        return progressService.getEnrollment(COURSE_ID, userId).getProgressPercentage();
    }

    /** The percentage the learner's own course-progress screen shows. */
    Double reportedCourseProgress(long userId) throws Exception {
        return progressService.getUserCourseProgress(COURSE_ID, userId).getCourseProgress();
    }

    // --- repository stubs ----------------------------------------------------------------

    private void stubShareLinkRepo() {
        when(shareLinkRepo.save(any(CourseShareLink.class))).thenAnswer(call -> {
            CourseShareLink link = call.getArgument(0);
            linksById.put(link.getId(), link);
            return link;
        });
        when(shareLinkRepo.findById(anyLong())).thenAnswer(call ->
                Optional.ofNullable(linksById.get(call.<Long>getArgument(0))));
        when(shareLinkRepo.findByShareToken(anyString())).thenAnswer(call -> linksById.values().stream()
                .filter(link -> call.getArgument(0).equals(link.getShareToken()))
                .findFirst());
        when(shareLinkRepo.findByCourseId(anyLong())).thenAnswer(call -> linksById.values().stream()
                .filter(link -> call.getArgument(0).equals(link.getCourseId()))
                .collect(Collectors.toList()));
        when(shareLinkRepo.findByCourseIdAndCreatedBy(anyLong(), anyLong())).thenAnswer(call -> linksById.values().stream()
                .filter(link -> call.getArgument(0).equals(link.getCourseId()))
                .filter(link -> call.getArgument(1).equals(link.getCreatedBy()))
                .collect(Collectors.toList()));
    }

    private void stubEnrollmentRepo() {
        when(enrollmentRepo.save(any(CourseEnrollment.class))).thenAnswer(call -> {
            CourseEnrollment enrollment = call.getArgument(0);
            enrollmentsByCourseUser.put(enrollmentKey(enrollment.getCourseId(), enrollment.getUserId()), enrollment);
            return enrollment;
        });
        when(enrollmentRepo.findByCourseIdAndUserId(anyLong(), anyLong())).thenAnswer(call ->
                Optional.ofNullable(enrollmentsByCourseUser.get(
                        enrollmentKey(call.getArgument(0), call.getArgument(1)))));
        when(enrollmentRepo.findByCourseId(anyLong())).thenAnswer(call -> enrollmentsByCourseUser.values().stream()
                .filter(enrollment -> call.getArgument(0).equals(enrollment.getCourseId()))
                .collect(Collectors.toList()));
        when(enrollmentRepo.findByUserId(anyLong())).thenAnswer(call -> enrollmentsByCourseUser.values().stream()
                .filter(enrollment -> call.getArgument(0).equals(enrollment.getUserId()))
                .collect(Collectors.toList()));
    }

    private void stubAllowlistRepo() {
        when(allowedUserRepo.saveAll(any())).thenAnswer(call -> {
            Iterable<CourseShareLinkAllowedUser> rows = call.getArgument(0);
            List<CourseShareLinkAllowedUser> saved = new ArrayList<>();
            for (CourseShareLinkAllowedUser row : rows) {
                allowlistRows.add(allowlistKey(row.getShareLinkId(), row.getUserId()));
                saved.add(row);
            }
            return saved;
        });
        when(allowedUserRepo.existsByShareLinkIdAndUserId(anyLong(), anyLong())).thenAnswer(call ->
                allowlistRows.contains(allowlistKey(call.getArgument(0), call.getArgument(1))));
        when(allowedUserRepo.findByShareLinkId(anyLong())).thenAnswer(call -> {
            String prefix = call.getArgument(0) + ":";
            return allowlistRows.stream()
                    .filter(row -> row.startsWith(prefix))
                    .map(row -> new CourseShareLinkAllowedUser(
                            Long.valueOf(row.substring(0, row.indexOf(':'))),
                            Long.valueOf(row.substring(row.indexOf(':') + 1))))
                    .collect(Collectors.toList());
        });
    }

    private void stubCourseAndUserRepos() {
        when(courseRepo.findById(anyLong())).thenAnswer(call ->
                COURSE_ID == call.<Long>getArgument(0) ? Optional.of(course) : Optional.empty());
        when(userRepo.findById(anyLong())).thenAnswer(call ->
                Optional.ofNullable(usersById.get(call.<Long>getArgument(0))));
        when(userRepo.findByUsername(anyString())).thenAnswer(call -> usersById.values().stream()
                .filter(user -> call.getArgument(0).equals(user.getUsername()))
                .findFirst()
                .orElse(null));
        when(userRepo.findAllById(any())).thenAnswer(call -> {
            Iterable<Long> ids = call.getArgument(0);
            List<Users> found = new ArrayList<>();
            for (Long id : ids) {
                Users user = usersById.get(id);
                if (user != null) {
                    found.add(user);
                }
            }
            return found;
        });
    }

    private void stubProgressRepos() {
        when(lessonRepo.countByCourseId(anyLong())).thenAnswer(call -> (long) totalLessons);
        when(lessonProgressRepo.save(any(LessonProgress.class))).thenAnswer(call -> {
            LessonProgress progress = call.getArgument(0);
            lessonProgress.put(progressKey(progress.getLessonId(), progress.getUserId()), progress);
            return progress;
        });
        when(lessonProgressRepo.findByLessonIdAndUserId(anyLong(), anyLong())).thenAnswer(call ->
                Optional.ofNullable(lessonProgress.get(progressKey(call.getArgument(0), call.getArgument(1)))));
        when(lessonProgressRepo.findByUserIdAndCourseId(anyLong(), anyLong())).thenAnswer(call ->
                lessonProgress.values().stream()
                        .filter(progress -> call.getArgument(0).equals(progress.getUserId()))
                        .filter(progress -> call.getArgument(1).equals(progress.getCourseId()))
                        .collect(Collectors.toList()));
        when(lessonProgressRepo.countByUserIdAndCourseIdAndIsCompletedTrue(anyLong(), anyLong())).thenAnswer(call ->
                (int) lessonProgress.values().stream()
                        .filter(progress -> call.getArgument(0).equals(progress.getUserId()))
                        .filter(progress -> call.getArgument(1).equals(progress.getCourseId()))
                        .filter(progress -> Boolean.TRUE.equals(progress.getIsCompleted()))
                        .count());
        when(lessonProgressRepo.findTopByUserIdAndCourseIdOrderByLastActivityAtDesc(anyLong(), anyLong()))
                .thenAnswer(call -> lessonProgress.values().stream()
                        .filter(progress -> call.getArgument(0).equals(progress.getUserId()))
                        .filter(progress -> call.getArgument(1).equals(progress.getCourseId()))
                        .findFirst());
    }

    private static String enrollmentKey(Long courseId, Long userId) {
        return courseId + ":" + userId;
    }

    private static String allowlistKey(Long shareLinkId, Long userId) {
        return shareLinkId + ":" + userId;
    }

    private static String progressKey(Long lessonId, Long userId) {
        return lessonId + ":" + userId;
    }

    private static void inject(Object target, String fieldName, Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException missing) {
                type = type.getSuperclass();
            } catch (IllegalAccessException blocked) {
                throw new IllegalStateException("Unable to inject " + fieldName, blocked);
            }
        }
        throw new IllegalStateException("No field named " + fieldName + " on " + target.getClass());
    }
}
