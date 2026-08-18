package com.sharing.service;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.sharing.exception.SharedCourseContentLockedException;
import com.sharing.model.CourseEnrollment;
import com.sharing.model.CourseShareLink;
import com.sharing.model.EnrollmentStatus;
import com.sharing.repo.CourseEnrollmentRepo;
import com.sharing.repo.CourseShareLinkRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SharedCourseAccessGuard {

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private CourseEnrollmentRepo courseEnrollmentRepo;

    @Autowired
    private CourseShareLinkRepo courseShareLinkRepo;

    public Course assertCourseShellAccess(Long courseId, Long userId) {
        Course course = getCourseOrThrow(courseId);

        if (course.getCreator().equals(userId)) {
            return course;
        }

        CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not enrolled in this course"));

        if (!hasActiveEnrollment(enrollment)) {
            throw new IllegalArgumentException("Enrollment is not active for this course");
        }

        return course;
    }

    public void assertContentAccessAllowed(Long courseId, Long userId) {
        Course course = assertCourseShellAccess(courseId, userId);

        if (course.getCreator().equals(userId)) {
            return;
        }

        if (!course.isActive()) {
            throw new SharedCourseContentLockedException(
                    "Course content is locked because this course was deactivated by the owner");
        }

        CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not enrolled in this course"));

        Long shareLinkId = enrollment.getShareLinkId();
        if (shareLinkId == null) {
            return;
        }

        CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                .orElseThrow(() -> new SharedCourseContentLockedException(
                        "Course content is locked because the original share link is no longer available"));

        if (!Boolean.TRUE.equals(shareLink.getIsActive())) {
            throw new SharedCourseContentLockedException(
                    "Course content is locked because this share link was deactivated by the course owner");
        }

        if (shareLink.isExpired()) {
            throw new SharedCourseContentLockedException(
                    "Course content is locked because this share link has expired");
        }
    }

    public ContentLockState getContentLockState(Long courseId, Long userId) {
        try {
            assertContentAccessAllowed(courseId, userId);
            return new ContentLockState(false, null);
        } catch (SharedCourseContentLockedException ex) {
            return new ContentLockState(true, ex.getMessage());
        }
    }

    private Course getCourseOrThrow(Long courseId) {
        return courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
    }

    private boolean hasActiveEnrollment(CourseEnrollment enrollment) {
        return EnrollmentStatus.ACTIVE.equals(enrollment.getStatus())
                || EnrollmentStatus.COMPLETED.equals(enrollment.getStatus());
    }

    public record ContentLockState(boolean locked, String reason) {
    }
}

