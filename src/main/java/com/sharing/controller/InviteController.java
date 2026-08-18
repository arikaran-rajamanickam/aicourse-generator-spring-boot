package com.sharing.controller;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.sharing.dto.EnrollmentResponse;
import com.sharing.model.CourseEnrollment;
import com.sharing.model.EnrollmentStatus;
import com.sharing.repo.CourseEnrollmentRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sharing/invites")
public class InviteController {

    private static final Logger LOGGER = Logger.getLogger(InviteController.class.getName());

    @Autowired
    private CourseEnrollmentRepo courseEnrollmentRepo;

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private UserRepo userRepo;

    /**
     * Get invites shared with the current user (incoming invites)
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getSharedWithMe(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "Fetching invites shared with user {0}", userId);
        try {
            List<CourseEnrollment> enrollments = courseEnrollmentRepo
                    .findByUserIdAndInviteType(userId, "DIRECT");

            List<EnrollmentResponse> responses = enrollments.stream()
                    .map(this::toEnrollmentResponseSafe)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Invites shared with me fetched successfully", responses));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching invites shared with me: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching invites shared with me: " + e.getMessage()));
        }
    }

    /**
     * Get invites shared by the current user (outgoing invites)
     */
    @GetMapping("/shared-by-me")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getSharedByMe(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "Fetching invites shared by user {0}", userId);
        try {
            List<CourseEnrollment> enrollments = courseEnrollmentRepo
                    .findByInvitedByAndInviteType(userId, "DIRECT");

            List<EnrollmentResponse> responses = enrollments.stream()
                    .map(this::toEnrollmentResponseSafe)
                    .filter(r -> r != null)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Invites shared by me fetched successfully", responses));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching invites shared by me: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching invites shared by me: " + e.getMessage()));
        }
    }

    /**
     * Get a summary of pending invites for the current user (for notification badge)
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> getInviteSummary(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "Fetching invite summary for user {0}", userId);
        try {
            int pendingInvites = courseEnrollmentRepo
                    .countUnreadInvites(userId, "DIRECT", "PENDING");

            java.util.Map<String, Integer> data = java.util.Collections.singletonMap("pendingInvitesCount", pendingInvites);
            return ResponseEntity.ok(ApiResponse.success("Invite summary fetched successfully", data));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching invite summary: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching invite summary: " + e.getMessage()));
        }
    }

    /**
     * Mark a single invite as read
     */
    @PutMapping("/{inviteId}/read")
    @Transactional
    public ResponseEntity<ApiResponse<EnrollmentResponse>> markInviteRead(@PathVariable Long inviteId,
                                                                          Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "User {0} marking invite {1} as read", new Object[]{userId, inviteId});
        try {
            CourseEnrollment enrollment = courseEnrollmentRepo.findById(inviteId)
                    .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

            if (!enrollment.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.failure("You are not allowed to act on this invite"));
            }

            if (!"DIRECT".equals(enrollment.getInviteType())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("Only DIRECT invites can be marked as read via this endpoint"));
            }

            enrollment.setIsRead(Boolean.TRUE);
            courseEnrollmentRepo.save(enrollment);

            EnrollmentResponse response = toEnrollmentResponse(enrollment);
            return ResponseEntity.ok(ApiResponse.success("Invite marked as read", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking invite as read: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error marking invite as read: " + e.getMessage()));
        }
    }

    /**
     * Accept a direct invite
     */
    @PutMapping("/{inviteId}/accept")
    @Transactional
    public ResponseEntity<ApiResponse<EnrollmentResponse>> acceptInvite(@PathVariable Long inviteId,
                                                                        Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "User {0} accepting invite {1}", new Object[]{userId, inviteId});
        try {
            CourseEnrollment enrollment = courseEnrollmentRepo.findById(inviteId)
                    .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

            if (!enrollment.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.failure("You are not allowed to act on this invite"));
            }

            if (!"DIRECT".equals(enrollment.getInviteType())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("Only DIRECT invites can be accepted via this endpoint"));
            }

            if ("ACCEPTED".equals(enrollment.getInviteStatus())) {
                // Idempotent: already accepted
                EnrollmentResponse response = toEnrollmentResponse(enrollment);
                return ResponseEntity.ok(ApiResponse.success("Invite already accepted", response));
            }

            enrollment.setInviteStatus("ACCEPTED");
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setIsRead(Boolean.TRUE);
            courseEnrollmentRepo.save(enrollment);

            EnrollmentResponse response = toEnrollmentResponse(enrollment);
            return ResponseEntity.ok(ApiResponse.success("Invite accepted successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error accepting invite: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error accepting invite: " + e.getMessage()));
        }
    }

    /**
     * Decline a direct invite
     */
    @PutMapping("/{inviteId}/decline")
    @Transactional
    public ResponseEntity<ApiResponse<EnrollmentResponse>> declineInvite(@PathVariable Long inviteId,
                                                                         Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "User {0} declining invite {1}", new Object[]{userId, inviteId});
        try {
            CourseEnrollment enrollment = courseEnrollmentRepo.findById(inviteId)
                    .orElseThrow(() -> new IllegalArgumentException("Invite not found"));

            if (!enrollment.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.failure("You are not allowed to act on this invite"));
            }

            if (!"DIRECT".equals(enrollment.getInviteType())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("Only DIRECT invites can be declined via this endpoint"));
            }

            if ("DECLINED".equals(enrollment.getInviteStatus())) {
                EnrollmentResponse response = toEnrollmentResponse(enrollment);
                return ResponseEntity.ok(ApiResponse.success("Invite already declined", response));
            }

            enrollment.setInviteStatus("DECLINED");
            enrollment.setStatus(EnrollmentStatus.DROPPED);
            enrollment.setIsRead(Boolean.TRUE);
            courseEnrollmentRepo.save(enrollment);

            EnrollmentResponse response = toEnrollmentResponse(enrollment);
            return ResponseEntity.ok(ApiResponse.success("Invite declined successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error declining invite: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error declining invite: " + e.getMessage()));
        }
    }

    /**
     * Mark all DIRECT invites for the current user as read (without changing status)
     */
    @PutMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> markAllRead(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getUser().getId();

        LOGGER.log(Level.INFO, "Marking all invites as read for user {0}", userId);
        try {
            List<CourseEnrollment> invites = courseEnrollmentRepo.findByUserIdAndInviteType(userId, "DIRECT");

            int updated = 0;
            for (CourseEnrollment enrollment : invites) {
                if (enrollment.getIsRead() == null || !enrollment.getIsRead()) {
                    enrollment.setIsRead(Boolean.TRUE);
                    courseEnrollmentRepo.save(enrollment);
                    updated++;
                }
            }

            java.util.Map<String, Integer> data = java.util.Collections.singletonMap("updatedCount", updated);
            return ResponseEntity.ok(ApiResponse.success("All invites marked as read", data));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking invites as read: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error marking invites as read: " + e.getMessage()));
        }
    }

    private EnrollmentResponse toEnrollmentResponseSafe(CourseEnrollment enrollment) {
        try {
            return toEnrollmentResponse(enrollment);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error mapping enrollment {0}: {1}", new Object[]{enrollment.getId(), e.getMessage()});
            return null;
        }
    }

    private EnrollmentResponse toEnrollmentResponse(CourseEnrollment enrollment) {
        Course course = courseRepo.findById(enrollment.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        String invitedByName = null;
        if (enrollment.getInvitedBy() != null) {
            invitedByName = userRepo.findById(enrollment.getInvitedBy())
                    .map(Users::getUsername)
                    .orElse("Unknown User");
        }

        int moduleCount = (course.getModules() != null) ? course.getModules().size() : 0;
        int lessonCount = 0;
        if (course.getModules() != null) {
            lessonCount = course.getModules().stream()
                    .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                    .sum();
        }

        String studentName = userRepo.findById(enrollment.getUserId())
                .map(Users::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElseGet(() -> userRepo.findById(enrollment.getUserId())
                        .map(Users::getUsername)
                        .orElse("Unknown User"));
        String studentHandle = userRepo.findById(enrollment.getUserId())
                .map(Users::getUsername)
                .orElse("");

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
                invitedByName,
                moduleCount,
                lessonCount,
                studentName,
                studentHandle
        );
    }
}

