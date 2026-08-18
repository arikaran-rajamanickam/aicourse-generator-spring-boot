package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_share_links")
public class CourseShareLink {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(name = "token", nullable = false, unique = true)
    private String shareToken;

    @Column(nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(name = "max_enrollments")
    private Integer maxEnrollments;

    @Column(name = "current_enrollments")
    private Integer currentEnrollments;

    @Column(name = "link_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShareLinkType linkType;

    // --- Constructors ---
    public CourseShareLink() {
    }

    public CourseShareLink(Long courseId, String shareToken, Long createdBy, ShareLinkType linkType) {
        this.id = SnowflakeIdGenerator.generateId();
        this.courseId = courseId;
        this.shareToken = shareToken;
        this.createdBy = createdBy;
        this.linkType = linkType;
        this.isActive = true;
        this.currentEnrollments = 0;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (currentEnrollments == null) {
            currentEnrollments = 0;
        }
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public Integer getMaxEnrollments() {
        return maxEnrollments;
    }

    public void setMaxEnrollments(Integer maxEnrollments) {
        this.maxEnrollments = maxEnrollments;
    }

    public Integer getCurrentEnrollments() {
        return currentEnrollments;
    }

    public void setCurrentEnrollments(Integer currentEnrollments) {
        this.currentEnrollments = currentEnrollments;
    }

    public ShareLinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(ShareLinkType linkType) {
        this.linkType = linkType;
    }

    // --- Helper methods ---
    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean canEnroll() {
        if (!isActive || isExpired()) {
            return false;
        }
        if (maxEnrollments != null) {
            return currentEnrollments <= maxEnrollments;
        }
        return true;
    }
}
