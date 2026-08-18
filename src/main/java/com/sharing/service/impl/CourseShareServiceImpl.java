package com.sharing.service.impl;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.sharing.dto.ShareLinkResponse;
import com.sharing.event.NotificationEvent;
import com.sharing.model.*;
import com.sharing.repo.CourseEnrollmentRepo;
import com.sharing.repo.CourseShareLinkAllowedUserRepo;
import com.sharing.repo.CourseShareLinkRepo;
import com.sharing.service.CourseShareService;
import com.sharing.service.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class CourseShareServiceImpl implements CourseShareService {

    private static final Logger LOGGER = Logger.getLogger(CourseShareServiceImpl.class.getName());
    private static final int TOKEN_LENGTH = 32;

    @Autowired
    private CourseShareLinkRepo courseShareLinkRepo;

    @Autowired
    private CourseRepo courseRepo;

    @Autowired
    private CourseEnrollmentRepo courseEnrollmentRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private CourseShareLinkAllowedUserRepo allowedUserRepo;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public ShareLinkResponse generateShareLink(Long courseId, Long creatorId, ShareLinkType linkType,
                                               OffsetDateTime expiresAt, Integer maxEnrollments,
                                               List<String> allowlistedUsers) throws Exception {
        LOGGER.log(Level.INFO, "Generating share link for course ID: {0}", courseId);

        try {
            // Verify course exists and user is creator
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            if (!course.getCreator().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to share this course");
            }

            // Generate unique token
            String shareToken = generateUniqueToken();

            // Create new share link
            CourseShareLink shareLink = new CourseShareLink(courseId, shareToken, creatorId, linkType);
            shareLink.setExpiresAt(expiresAt);
            shareLink.setMaxEnrollments(maxEnrollments);

            CourseShareLink savedLink = courseShareLinkRepo.save(shareLink);

            if (ShareLinkType.PRIVATE.equals(linkType)) {
                List<Long> allowedUserIds = resolveAllowedUserIds(allowlistedUsers);
                if (allowedUserIds.isEmpty()) {
                    throw new IllegalArgumentException("At least one valid user is required for a PRIVATE share link");
                }

                List<CourseShareLinkAllowedUser> allowlistRows = allowedUserIds.stream()
                        .map(userId -> new CourseShareLinkAllowedUser(savedLink.getId(), userId))
                        .collect(Collectors.toList());
                allowedUserRepo.saveAll(allowlistRows);
            }
            LOGGER.log(Level.INFO, "Share link created with token: {0}", shareToken);

            return mapToResponse(savedLink);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating share link: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ShareLinkResponse> getCourseShareLinks(Long courseId, Long creatorId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching share links for course ID: {0}", courseId);

        try {
            // Verify course exists and user is creator
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            if (!course.getCreator().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to view share links for this course");
            }

            List<CourseShareLink> shareLinks = courseShareLinkRepo.findByCourseIdAndCreatedBy(courseId, creatorId);
            return shareLinks.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching share links: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    public ShareLinkResponse getShareLinkByToken(String token, Long requestingUserId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching share link by token");

        try {
            CourseShareLink shareLink = courseShareLinkRepo.findByShareToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired share link"));

            if (!shareLink.canEnroll()) {
                throw new IllegalArgumentException("Share link is no longer valid");
            }

            // ✅ CHECK: Course must be active
            Course course = courseRepo.findById(shareLink.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            if (!course.isActive()) {
                LOGGER.log(Level.WARNING, "Share link resolution failed: Course {0} is deactivated", shareLink.getCourseId());
                throw new IllegalArgumentException("This course has been deactivated and is no longer available");
            }

            if (ShareLinkType.DIRECT_INVITE.equals(shareLink.getLinkType())) {
                if (requestingUserId == null) {
                    throw new IllegalArgumentException("Login required to access this private share link");
                }

                boolean allowed = allowedUserRepo.existsByShareLinkIdAndUserId(shareLink.getId(), requestingUserId);
                if (!allowed) {
                    throw new IllegalArgumentException("You are not allowed to access this private share link");
                }
            }

            return mapToResponse(shareLink);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching share link: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void deactivateShareLink(Long shareLinkId, Long creatorId) throws Exception {
        LOGGER.log(Level.INFO, "Deactivating share link ID: {0}", shareLinkId);

        try {
            CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

            if (!shareLink.getCreatedBy().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to deactivate this share link");
            }
            if (shareLink.getIsActive() == Boolean.FALSE) {
                return;
            }
            shareLink.setIsActive(false);
            courseShareLinkRepo.save(shareLink);
            LOGGER.log(Level.INFO, "Share link deactivated successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating share link: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void activateShareLink(Long shareLinkId, Long creatorId) throws Exception {
        LOGGER.log(Level.INFO, "Activating share link ID: {0}", shareLinkId);

        try {
            CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

            if (!shareLink.getCreatedBy().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to activate this share link");
            }
            if (shareLink.getIsActive() == Boolean.TRUE) {
                return;
            }
            shareLink.setIsActive(true);
            courseShareLinkRepo.save(shareLink);
            LOGGER.log(Level.INFO, "Share link activated successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating share link: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void sendDirectInvite(Long courseId, Long creatorId, List<String> userEmails) throws Exception {
        LOGGER.log(Level.INFO, "Sending direct invites for course ID: {0}", courseId);

        try {
            // Verify course exists and user is creator
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));

            if (!course.getCreator().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to send invites for this course");
            }

            if (userEmails == null || userEmails.isEmpty()) {
                throw new IllegalArgumentException("At least one username/email identifier is required");
            }

            // Generate a private share link for direct invites
            String shareToken = generateUniqueToken();
            CourseShareLink inviteLink = new CourseShareLink(courseId, shareToken, creatorId, ShareLinkType.DIRECT_INVITE);
            CourseShareLink savedInviteLink = courseShareLinkRepo.save(inviteLink);

            List<String> invalidIdentifiers = new ArrayList<>();
            int invitesPrepared = 0;

            for (String identifier : userEmails) {
                String normalized = identifier == null ? null : identifier.trim();
                if (normalized == null || normalized.isBlank()) {
                    continue;
                }

                Users targetUser = resolveUser(normalized);
                if (targetUser == null) {
                    invalidIdentifiers.add(normalized);
                    continue;
                }

                if (Objects.equals(targetUser.getId(), creatorId)) {
                    continue;
                }

                CourseEnrollment enrollment = courseEnrollmentRepo.findByCourseIdAndUserId(courseId, targetUser.getId())
                        .orElseGet(CourseEnrollment::new);

                enrollment.setCourseId(courseId);
                enrollment.setUserId(targetUser.getId());
                enrollment.setShareLinkId(savedInviteLink.getId());
                enrollment.setInvitedBy(creatorId);
                enrollment.setInviteType("DIRECT");
                enrollment.setInviteStatus("PENDING");
                enrollment.setStatus(EnrollmentStatus.SUSPENDED);
                enrollment.setIsRead(Boolean.FALSE);

                courseEnrollmentRepo.save(enrollment);
                invitesPrepared++;

                // Send real-time notification via SSE
                Users inviter = userRepo.findById(creatorId).orElse(null);
                String inviterName = inviter != null ? inviter.getDisplayName() : "Someone";
                if (inviterName == null && inviter != null) inviterName = inviter.getUsername();

                notificationService.sendNotification(targetUser.getId(), NotificationEvent.builder()
                        .type("INVITE_RECEIVED")
                        .userId(targetUser.getId())
                        .message(String.format("%s invited you to join \"%s\"", inviterName, course.getTitle()))
                        .payload(Map.of(
                                "courseId", courseId,
                                "courseName", course.getTitle(),
                                "invitedBy", creatorId
                        ))
                        .build());
            }

            if (invitesPrepared == 0) {
                throw new IllegalArgumentException("No valid users found to invite");
            }

            if (!invalidIdentifiers.isEmpty()) {
                throw new IllegalArgumentException("Some users were not found: " + String.join(", ", invalidIdentifiers));
            }

            // TODO: Send email invites using email service
            // For now, just log the invites
            LOGGER.log(Level.INFO, "Direct invites prepared for {0} users", invitesPrepared);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending direct invites: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void revokeShareLink(Long shareLinkId, Long creatorId) throws Exception {
        LOGGER.log(Level.INFO, "Revoking share link ID: {0}", shareLinkId);

        try {
            CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

            if (!shareLink.getCreatedBy().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to revoke this share link");
            }

            courseShareLinkRepo.deleteById(shareLinkId);
            LOGGER.log(Level.INFO, "Share link revoked successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error revoking share link: {0}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public ShareLinkResponse updateShareLink(Long shareLinkId, Long creatorId, OffsetDateTime expiresAt,
                                             Integer maxEnrollments) throws Exception {
        LOGGER.log(Level.INFO, "Updating share link ID: {0}", shareLinkId);

        try {
            CourseShareLink shareLink = courseShareLinkRepo.findById(shareLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("Share link not found"));

            if (!shareLink.getCreatedBy().equals(creatorId)) {
                throw new IllegalArgumentException("User is not authorized to update this share link");
            }

            if (expiresAt != null) {
                shareLink.setExpiresAt(expiresAt);
            }
            if (maxEnrollments != null) {
                shareLink.setMaxEnrollments(maxEnrollments);
            }

            CourseShareLink updatedLink = courseShareLinkRepo.save(shareLink);
            LOGGER.log(Level.INFO, "Share link updated successfully");
            return mapToResponse(updatedLink);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating share link: {0}", e.getMessage());
            throw e;
        }
    }

    // --- Helper methods ---
    private String generateUniqueToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private Users resolveUser(String identifier) {
        Users user = userRepo.findByUsername(identifier);
        if (user != null) {
            return user;
        }

        // If email-like identifier is provided, try local-part as username fallback.
        int atIdx = identifier.indexOf('@');
        if (atIdx > 0) {
            String localPart = identifier.substring(0, atIdx);
            return userRepo.findByUsername(localPart);
        }
        return null;
    }

    private ShareLinkResponse mapToResponse(CourseShareLink shareLink) {
        String shareUrl = String.format("/join/%s", shareLink.getShareToken());
        List<String> allowedUsernames = null;

        if (ShareLinkType.PRIVATE.equals(shareLink.getLinkType())) {
            List<CourseShareLinkAllowedUser> allowlist = allowedUserRepo.findByShareLinkId(shareLink.getId());
            Set<Long> userIds = allowlist.stream()
                    .map(CourseShareLinkAllowedUser::getUserId)
                    .collect(Collectors.toSet());
            allowedUsernames = userRepo.findAllById(userIds).stream()
                    .map(Users::getUsername)
                    .collect(Collectors.toList());
        }

        Course course = courseRepo.findById(shareLink.getCourseId()).orElse(null);
        String courseName = course != null ? course.getTitle() : "Unknown Course";
        String courseDescription = course != null ? course.getDescription() : "";
        int moduleCount = (course != null && course.getModules() != null) ? course.getModules().size() : 0;
        // Assuming course has a way to get total lessons, or count them from modules
        int lessonCount = 0;
        if (course != null && course.getModules() != null) {
            lessonCount = course.getModules().stream()
                    .mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0)
                    .sum();
        }

        Users inviter = userRepo.findById(shareLink.getCreatedBy()).orElse(null);
        String inviterUsername = inviter != null ? inviter.getUsername() : "Someone";

        return new ShareLinkResponse(
                shareLink.getId(),
                shareLink.getShareToken(),
                shareLink.getCourseId(),
                shareLink.getLinkType(),
                shareLink.getCreatedAt(),
                shareLink.getExpiresAt(),
                shareLink.getIsActive(),
                shareLink.getCurrentEnrollments(),
                shareLink.getMaxEnrollments(),
                shareUrl,
                allowedUsernames,
                courseName,
                courseDescription,
                inviterUsername,
                moduleCount,
                lessonCount
        );
    }

    private List<Long> resolveAllowedUserIds(List<String> identifiers) {
        if (identifiers == null) {
            return List.of();
        }

        Set<Long> allowedIds = new HashSet<>();
        List<String> invalid = new ArrayList<>();

        for (String raw : identifiers) {
            if (raw == null) {
                continue;
            }
            String identifier = raw.trim();
            if (identifier.isBlank()) {
                continue;
            }

            Users user = resolveUser(identifier);
            if (user == null) {
                invalid.add(identifier);
                continue;
            }

            allowedIds.add(user.getId());
        }

        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Unknown users for PRIVATE link: " + String.join(", ", invalid));
        }

        return new ArrayList<>(allowedIds);
    }
}

