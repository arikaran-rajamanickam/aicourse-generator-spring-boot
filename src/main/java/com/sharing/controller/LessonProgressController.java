package com.sharing.controller;

import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.sharing.dto.*;
import com.sharing.exception.SharedCourseContentLockedException;
import com.sharing.model.EnrollmentStatus;
import com.sharing.service.LessonProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/progress")
public class LessonProgressController {

    private static final Logger LOGGER = Logger.getLogger(LessonProgressController.class.getName());

    @Autowired
    private LessonProgressService lessonProgressService;

    /**
     * Mark a lesson as complete
     */
    @PutMapping("/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Void>> markLessonComplete(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to mark lesson {0} complete", lessonId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            lessonProgressService.markLessonComplete(lessonId, courseId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Lesson marked as complete successfully");
            return ResponseEntity.ok(ApiResponse.success("Lesson marked as complete", null));
        } catch (SharedCourseContentLockedException e) {
            LOGGER.log(Level.INFO, "Content access locked while marking lesson complete: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking lesson complete: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error marking lesson complete: " + e.getMessage()));
        }
    }

    /**
     * Mark a lesson as incomplete
     */
    @PutMapping("/lessons/{lessonId}/incomplete")
    public ResponseEntity<ApiResponse<Void>> markLessonIncomplete(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to mark lesson {0} incomplete", lessonId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            lessonProgressService.markLessonIncomplete(lessonId, courseId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Lesson marked as incomplete successfully");
            return ResponseEntity.ok(ApiResponse.success("Lesson marked as incomplete", null));
        } catch (SharedCourseContentLockedException e) {
            LOGGER.log(Level.INFO, "Content access locked while marking lesson incomplete: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking lesson incomplete: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error marking lesson incomplete: " + e.getMessage()));
        }
    }

    /**
     * Start a lesson session
     */
    @PostMapping("/lessons/{lessonId}/session/start")
    public ResponseEntity<ApiResponse<Void>> startLessonSession(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to start lesson session for {0}", lessonId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            lessonProgressService.startLessonSession(lessonId, courseId, principal.getUser().getId());
            return ResponseEntity.ok(ApiResponse.success("Lesson session started", null));
        } catch (SharedCourseContentLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting lesson session: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error starting lesson session: " + e.getMessage()));
        }
    }

    /**
     * Stop a lesson session
     */
    @PostMapping("/lessons/{lessonId}/session/stop")
    public ResponseEntity<ApiResponse<Void>> stopLessonSession(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to stop lesson session for {0}", lessonId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            lessonProgressService.stopLessonSession(lessonId, courseId, principal.getUser().getId());
            return ResponseEntity.ok(ApiResponse.success("Lesson session stopped", null));
        } catch (SharedCourseContentLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error stopping lesson session: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error stopping lesson session: " + e.getMessage()));
        }
    }

    /**
     * Record a quiz attempt
     */
    @PostMapping("/lessons/{lessonId}/quiz-attempts")
    public ResponseEntity<ApiResponse<Void>> recordQuizAttempt(
            @PathVariable Long lessonId,
            @RequestParam Long courseId,
            @RequestBody QuizAttemptRequest payload,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to record quiz attempt for {0}", lessonId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            if (payload == null || payload.getQuizIndex() == null || payload.getCorrect() == null) {
                throw new IllegalArgumentException("quizIndex and correct are required");
            }
            lessonProgressService.recordQuizAttempt(
                    lessonId,
                    courseId,
                    principal.getUser().getId(),
                    payload.getQuizIndex(),
                    payload.getCorrect()
            );
            return ResponseEntity.ok(ApiResponse.success("Quiz attempt recorded", null));
        } catch (SharedCourseContentLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error recording quiz attempt: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error recording quiz attempt: " + e.getMessage()));
        }
    }

    /**
     * Get user's progress in a specific course
     */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getCourseProgress(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch progress for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            CourseProgressResponse response = lessonProgressService.getUserCourseProgress(courseId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Course progress fetched successfully");
            return ResponseEntity.ok(ApiResponse.success("Course progress fetched successfully", response));
        } catch (SharedCourseContentLockedException e) {
            LOGGER.log(Level.INFO, "Content access locked while fetching course progress: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching course progress: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching course progress: " + e.getMessage()));
        }
    }

    /**
     * Get completed lesson IDs for the current user in a course
     */
    @GetMapping("/courses/{courseId}/completed-lessons")
    public ResponseEntity<ApiResponse<List<Long>>> getCompletedLessonIds(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch completed lesson IDs for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            List<Long> response = lessonProgressService.getCompletedLessonIds(courseId, principal.getUser().getId());
            return ResponseEntity.ok(ApiResponse.success("Completed lesson IDs fetched successfully", response));
        } catch (SharedCourseContentLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching completed lesson IDs: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching completed lesson IDs: " + e.getMessage()));
        }
    }

    /**
     * Get all user's course progress
     */
    @GetMapping("/my-progress")
    public ResponseEntity<ApiResponse<List<CourseProgressResponse>>> getUserAllProgress(
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch all user progress");
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            List<CourseProgressResponse> response = lessonProgressService.getUserAllProgress(principal.getUser().getId());

            LOGGER.log(Level.INFO, "All user progress fetched successfully");
            return ResponseEntity.ok(ApiResponse.success("All user progress fetched successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching user progress: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching user progress: " + e.getMessage()));
        }
    }

    /**
     * Get user's enrollment in a course
     */
    @GetMapping("/enrollments/{courseId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollment(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch enrollment for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            EnrollmentResponse response = lessonProgressService.getEnrollment(courseId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Enrollment fetched successfully");
            return ResponseEntity.ok(ApiResponse.success("Enrollment fetched successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching enrollment: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching enrollment: " + e.getMessage()));
        }
    }

    /**
     * Get all enrollments for a course (admin/creator only)
     */
    @GetMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<ApiResponse<PagedResponse<EnrollmentResponse>>> getCourseEnrollments(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LOGGER.log(Level.INFO, "Request received to fetch enrollments for course: {0}", courseId);
        try {
            PagedResponse<EnrollmentResponse> response = lessonProgressService.getCourseEnrollmentsPaged(courseId, page, size);

            LOGGER.log(Level.INFO, "Course enrollments fetched successfully");
            return ResponseEntity.ok(ApiResponse.success("Course enrollments fetched successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching course enrollments: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching course enrollments: " + e.getMessage()));
        }
    }

    /**
     * Update enrollment status
     */
    @PutMapping("/enrollments/{courseId}/status")
    public ResponseEntity<ApiResponse<Void>> updateEnrollmentStatus(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to update enrollment status for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String statusStr = payload.get("status");
            EnrollmentStatus status = EnrollmentStatus.valueOf(statusStr);

            lessonProgressService.updateEnrollmentStatus(courseId, principal.getUser().getId(), status);

            LOGGER.log(Level.INFO, "Enrollment status updated successfully");
            return ResponseEntity.ok(ApiResponse.success("Enrollment status updated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating enrollment status: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error updating enrollment status: " + e.getMessage()));
        }
    }

    /**
     * Get shared course usage report
     */
    @GetMapping("/courses/{courseId}/users/{userId}/report")
    public ResponseEntity<ApiResponse<SharedCourseUsageResponse>> getSharedCourseUsage(
            @PathVariable Long courseId,
            @PathVariable Long userId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch shared course usage for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            SharedCourseUsageResponse response = lessonProgressService
                    .getSharedCourseUsage(courseId, userId, principal.getUser().getId());
            return ResponseEntity.ok(ApiResponse.success("Shared course usage fetched successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching shared course usage: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching shared course usage: " + e.getMessage()));
        }
    }

    /**
     * Get course leaderboard (accessible by creator or any enrolled user)
     */
    @GetMapping("/courses/{courseId}/leaderboard")
    public ResponseEntity<ApiResponse<PagedResponse<CourseLeaderboardEntry>>> getCourseLeaderboard(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch leaderboard for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            PagedResponse<CourseLeaderboardEntry> entries = lessonProgressService
                    .getCourseLeaderboardPaged(courseId, principal.getUser().getId(), page, size);
            return ResponseEntity.ok(ApiResponse.success("Leaderboard fetched successfully", entries));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching leaderboard: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching leaderboard: " + e.getMessage()));
        }
    }
}
