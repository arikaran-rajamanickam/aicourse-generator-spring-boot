package com.sharing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sharing.model.ShareLinkType;

import java.time.OffsetDateTime;

public class ShareLinkResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String shareToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long courseId;

    private ShareLinkType linkType;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private Boolean isActive;
    private Integer currentEnrollments;
    private Integer maxEnrollments;
    private String shareUrl;
    private java.util.List<String> allowedUsers;
    private String courseName;
    private String courseDescription;
    private String inviterUsername;
    private int moduleCount;
    private int lessonCount;

    // --- Constructors ---
    public ShareLinkResponse() {
    }

    public ShareLinkResponse(Long id, String shareToken, Long courseId, ShareLinkType linkType,
                             OffsetDateTime createdAt, OffsetDateTime expiresAt, Boolean isActive,
                             Integer currentEnrollments, Integer maxEnrollments, String shareUrl,
                             java.util.List<String> allowedUsers, String courseName,
                             String courseDescription, String inviterUsername, int moduleCount, int lessonCount) {
        this.id = id;
        this.shareToken = shareToken;
        this.courseId = courseId;
        this.linkType = linkType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
        this.currentEnrollments = currentEnrollments;
        this.maxEnrollments = maxEnrollments;
        this.shareUrl = shareUrl;
        this.allowedUsers = allowedUsers;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.inviterUsername = inviterUsername;
        this.moduleCount = moduleCount;
        this.lessonCount = lessonCount;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public ShareLinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(ShareLinkType linkType) {
        this.linkType = linkType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getCurrentEnrollments() {
        return currentEnrollments;
    }

    public void setCurrentEnrollments(Integer currentEnrollments) {
        this.currentEnrollments = currentEnrollments;
    }

    public Integer getMaxEnrollments() {
        return maxEnrollments;
    }

    public void setMaxEnrollments(Integer maxEnrollments) {
        this.maxEnrollments = maxEnrollments;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public java.util.List<String> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(java.util.List<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    public void setModuleCount(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = lessonCount;
    }
}
