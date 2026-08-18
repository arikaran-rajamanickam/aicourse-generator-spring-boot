package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_share_link_allowed_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"share_link_id", "user_id"})
})
public class CourseShareLinkAllowedUser {

    @Id
    private Long id;

    @Column(name = "share_link_id", nullable = false)
    private Long shareLinkId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public CourseShareLinkAllowedUser() {
    }

    public CourseShareLinkAllowedUser(Long shareLinkId, Long userId) {
        this.id = SnowflakeIdGenerator.generateId();
        this.shareLinkId = shareLinkId;
        this.userId = userId;
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
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShareLinkId() {
        return shareLinkId;
    }

    public void setShareLinkId(Long shareLinkId) {
        this.shareLinkId = shareLinkId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

