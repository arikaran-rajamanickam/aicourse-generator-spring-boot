package com.sharing.service;

import com.sharing.dto.ShareLinkResponse;
import com.sharing.model.ShareLinkType;

import java.time.OffsetDateTime;
import java.util.List;

public interface CourseShareService {

    /**
     * Generate a new share link for a course
     */
    ShareLinkResponse generateShareLink(Long courseId, Long creatorId, ShareLinkType linkType,
                                        OffsetDateTime expiresAt, Integer maxEnrollments,
                                        List<String> allowlistedUsers) throws Exception;

    /**
     * Get all share links for a course
     */
    List<ShareLinkResponse> getCourseShareLinks(Long courseId, Long creatorId) throws Exception;

    /**
     * Get a specific share link by token
     */
    ShareLinkResponse getShareLinkByToken(String token, Long requestingUserId) throws Exception;

    /**
     * Deactivate a share link
     */
    void deactivateShareLink(Long shareLinkId, Long creatorId) throws Exception;

    /**
     * Activate a share link
     */
    void activateShareLink(Long shareLinkId, Long creatorId) throws Exception;

    /**
     * Send direct invitation to users
     */
    void sendDirectInvite(Long courseId, Long creatorId, List<String> userEmails) throws Exception;

    /**
     * Revoke a share link
     */
    void revokeShareLink(Long shareLinkId, Long creatorId) throws Exception;

    /**
     * Update share link properties
     */
    ShareLinkResponse updateShareLink(Long shareLinkId, Long creatorId, OffsetDateTime expiresAt,
                                      Integer maxEnrollments) throws Exception;
}

