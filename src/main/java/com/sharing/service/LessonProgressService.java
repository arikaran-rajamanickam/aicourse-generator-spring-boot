package com.sharing.service;

import com.sharing.dto.*;
import com.sharing.model.EnrollmentStatus;

import java.util.List;

public interface LessonProgressService {

    /**
     * Mark a lesson as complete for a user
     */
    void markLessonComplete(Long lessonId, Long courseId, Long userId) throws Exception;

    /**
     * Mark a lesson as incomplete for a user
     */
    void markLessonIncomplete(Long lessonId, Long courseId, Long userId) throws Exception;

    void startLessonSession(Long lessonId, Long courseId, Long userId) throws Exception;

    void stopLessonSession(Long lessonId, Long courseId, Long userId) throws Exception;

    void recordQuizAttempt(Long lessonId, Long courseId, Long userId, int quizIndex, boolean correct) throws Exception;

    SharedCourseUsageResponse getSharedCourseUsage(Long courseId, Long childUserId, Long creatorId) throws Exception;

    /**
     * Get user's progress in a course
     */
    CourseProgressResponse getUserCourseProgress(Long courseId, Long userId) throws Exception;

    /**
     * Get completed lesson IDs for the current user in a course
     */
    List<Long> getCompletedLessonIds(Long courseId, Long userId) throws Exception;

    /**
     * Get all user's course progress
     */
    List<CourseProgressResponse> getUserAllProgress(Long userId) throws Exception;

    /**
     * Get course enrollment for a user
     */
    EnrollmentResponse getEnrollment(Long courseId, Long userId) throws Exception;

    /**
     * Get all enrollments for a course
     */
    List<EnrollmentResponse> getCourseEnrollments(Long courseId) throws Exception;

    PagedResponse<EnrollmentResponse> getCourseEnrollmentsPaged(Long courseId, int page, int size) throws Exception;

    /**
     * Enroll user in a course
     */
    EnrollmentResponse enrollUserInCourse(Long courseId, Long userId, Long shareLinkId) throws Exception;

    /**
     * Update enrollment status
     */
    void updateEnrollmentStatus(Long courseId, Long userId, EnrollmentStatus status) throws Exception;

    /**
     * Get user's completed lessons count in a course
     */
    int getCompletedLessonsCount(Long courseId, Long userId) throws Exception;

    /**
     * Get total lessons in a course
     */
    int getTotalLessonsInCourse(Long courseId) throws Exception;

    /**
     * Get course leaderboard
     */
    List<CourseLeaderboardEntry> getCourseLeaderboard(Long courseId, Long requestingUserId) throws Exception;

    PagedResponse<CourseLeaderboardEntry> getCourseLeaderboardPaged(Long courseId, Long requestingUserId, int page, int size) throws Exception;
}
