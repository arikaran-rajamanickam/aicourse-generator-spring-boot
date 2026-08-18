package com.sharing.controller;

import com.aicourse.model.Course;
import com.aicourse.repo.CourseRepo;
import com.aicourse.utils.api.ApiResponse;
import com.auth.model.UserPrincipal;
import com.sharing.dto.ShareLinkResponse;
import com.sharing.model.ShareLinkType;
import com.sharing.service.CourseShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/courses/{courseId}/share")
public class CourseShareController {

    private static final Logger LOGGER = Logger.getLogger(CourseShareController.class.getName());

    @Autowired
    private CourseShareService courseShareService;
    @Autowired
    private CourseRepo courseRepo;

    /**
     * Generate a new share link for the course
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> generateShareLink(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to generate share link for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            String linkTypeStr = (String) payload.getOrDefault("linkType", "PUBLIC");
            ShareLinkType linkType = ShareLinkType.valueOf(linkTypeStr);

            String expiresAtStr = (String) payload.get("expiresAt");
            OffsetDateTime expiresAt = expiresAtStr != null ? OffsetDateTime.parse(expiresAtStr) : null;

            Integer maxEnrollments = (Integer) payload.get("maxEnrollments");

            List<String> allowlistedUsers = new ArrayList<>();
            Object allowlistRaw = payload.get("allowedUsers");
            if (allowlistRaw instanceof List<?> identifiers) {
                for (Object identifier : identifiers) {
                    if (identifier != null) {
                        allowlistedUsers.add(String.valueOf(identifier));
                    }
                }
            }

            ShareLinkResponse response = courseShareService.generateShareLink(
                    courseId,
                    principal.getUser().getId(),
                    linkType,
                    expiresAt,
                    maxEnrollments,
                    allowlistedUsers
            );

            LOGGER.log(Level.INFO, "Share link generated successfully");
            return ResponseEntity.ok(ApiResponse.success("Share link generated successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating share link: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error generating share link: " + e.getMessage()));
        }
    }

    /**
     * Get all share links for a course
     */
    @GetMapping("/links")
    public ResponseEntity<ApiResponse<List<ShareLinkResponse>>> getCourseShareLinks(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to fetch share links for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            List<ShareLinkResponse> response = courseShareService.getCourseShareLinks(courseId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Share links fetched successfully");
            return ResponseEntity.ok(ApiResponse.success("Share links fetched successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching share links: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error fetching share links: " + e.getMessage()));
        }
    }

    /**
     * Deactivate a share link
     */
    @PutMapping("/links/{shareLinkId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateShareLink(
            @PathVariable Long courseId,
            @PathVariable Long shareLinkId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to deactivate share link: {0}", shareLinkId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            courseShareService.deactivateShareLink(shareLinkId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Share link deactivated successfully");
            return ResponseEntity.ok(ApiResponse.success("Share link deactivated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating share link: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error deactivating share link: " + e.getMessage()));
        }
    }

    /**
     * Deactivate all share link
     */
    @PutMapping("/links/deactivate-all")
    public ResponseEntity<ApiResponse<Void>> deactivateAllShareLink(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to deactivate all share links for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            List<ShareLinkResponse> links = courseShareService.getCourseShareLinks(courseId, principal.getUser().getId());
            for (ShareLinkResponse link : links) {
                courseShareService.deactivateShareLink(link.getId(), principal.getUser().getId());
            }

            LOGGER.log(Level.INFO, "All share links deactivated successfully for course: {0}", courseId);
            return ResponseEntity.ok(ApiResponse.success("All share links deactivated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating all share links for course {0}: {1}",
                    new Object[]{courseId, e.getMessage()});
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error deactivating all share links: " + e.getMessage()));
        }
    }

    /**
     * Activate all share link
     */
    @PutMapping("/links/activate-all")
    public ResponseEntity<ApiResponse<Void>> activateAllShareLink(
            @PathVariable Long courseId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to activate all share links for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            List<ShareLinkResponse> links = courseShareService.getCourseShareLinks(courseId, principal.getUser().getId());
            for (ShareLinkResponse link : links) {
                courseShareService.activateShareLink(link.getId(), principal.getUser().getId());
            }

            LOGGER.log(Level.INFO, "All share links activated successfully for course: {0}", courseId);
            return ResponseEntity.ok(ApiResponse.success("All share links activated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating all share links for course {0}: {1}",
                    new Object[]{courseId, e.getMessage()});
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error activating all share links: " + e.getMessage()));
        }
    }

    /**
     * Activate a share link
     */
    @PutMapping("/links/{shareLinkId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateShareLink(
            @PathVariable Long courseId,
            @PathVariable Long shareLinkId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to activate share link: {0}", shareLinkId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Course curCourse = courseRepo.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId + " for activating share link"));
            if (!curCourse.isActive()) curCourse.setActive(true);
            courseShareService.activateShareLink(shareLinkId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Share link activated successfully");
            return ResponseEntity.ok(ApiResponse.success("Share link activated successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating share link: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error activating share link: " + e.getMessage()));
        }
    }

    /**
     * Revoke a share link (delete it)
     */
    @DeleteMapping("/links/{shareLinkId}")
    public ResponseEntity<ApiResponse<Void>> revokeShareLink(
            @PathVariable Long courseId,
            @PathVariable Long shareLinkId,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to revoke share link: {0}", shareLinkId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            courseShareService.revokeShareLink(shareLinkId, principal.getUser().getId());

            LOGGER.log(Level.INFO, "Share link revoked successfully");
            return ResponseEntity.ok(ApiResponse.success("Share link revoked successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error revoking share link: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error revoking share link: " + e.getMessage()));
        }
    }

    /**
     * Update share link properties
     */
    @PutMapping("/links/{shareLinkId}")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> updateShareLink(
            @PathVariable Long courseId,
            @PathVariable Long shareLinkId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to update share link: {0}", shareLinkId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            String expiresAtStr = (String) payload.get("expiresAt");
            OffsetDateTime expiresAt = expiresAtStr != null ? OffsetDateTime.parse(expiresAtStr) : null;

            Integer maxEnrollments = (Integer) payload.get("maxEnrollments");

            ShareLinkResponse response = courseShareService.updateShareLink(
                    shareLinkId,
                    principal.getUser().getId(),
                    expiresAt,
                    maxEnrollments
            );

            LOGGER.log(Level.INFO, "Share link updated successfully");
            return ResponseEntity.ok(ApiResponse.success("Share link updated successfully", response));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating share link: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error updating share link: " + e.getMessage()));
        }
    }

    /**
     * Send direct invites to users
     */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Void>> sendDirectInvite(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {

        LOGGER.log(Level.INFO, "Request received to send direct invites for course: {0}", courseId);
        try {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

            @SuppressWarnings("unchecked")
            List<String> identifiers = new ArrayList<>();
            Object emailsPayload = payload.get("emails");
            if (emailsPayload instanceof List<?> emailList) {
                for (Object raw : emailList) {
                    if (raw != null) {
                        identifiers.add(String.valueOf(raw));
                    }
                }
            }

            Object identifier = payload.get("identifier");
            if (identifier != null) {
                identifiers.add(String.valueOf(identifier));
            }

            courseShareService.sendDirectInvite(courseId, principal.getUser().getId(), identifiers);

            LOGGER.log(Level.INFO, "Direct invites sent successfully");
            return ResponseEntity.ok(ApiResponse.success("Direct invites sent successfully", null));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending direct invites: {0}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Error sending direct invites: " + e.getMessage()));
        }
    }

}








