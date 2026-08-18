package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id", "user_id"})
})
public class CourseEnrollment {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "share_link_id")
    private Long shareLinkId;

    // Legacy sharing schema columns still required by DB constraints.
    @Column(name = "invited_by")
    private Long invitedBy;

    @Column(name = "invite_type", nullable = false)
    private String inviteType;

    @Column(name = "status", nullable = false)
    private String inviteStatus;

    @Column(name = "enrollment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    @Column(name = "progress_percentage")
    private Double progressPercentage;

    // Whether the invite/notification has been read by the user (for DIRECT invites)
    @Column(name = "is_read")
    private Boolean isRead;

    // --- Constructors ---
    public CourseEnrollment() {
    }

    public CourseEnrollment(Long courseId, Long userId, Long shareLinkId) {
        this.id = SnowflakeIdGenerator.generateId();
        this.courseId = courseId;
        this.userId = userId;
        this.shareLinkId = shareLinkId;
        this.inviteType = "LINK";
        this.inviteStatus = "ACCEPTED";
        this.status = EnrollmentStatus.ACTIVE;
        this.progressPercentage = 0.0;
        this.enrolledAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (enrolledAt == null) {
            enrolledAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = EnrollmentStatus.ACTIVE;
        }
        if (inviteType == null) {
            inviteType = "LINK";
        }
        if (inviteStatus == null) {
            inviteStatus = "ACCEPTED";
        }
        if (progressPercentage == null) {
            progressPercentage = 0.0;
        }
        if (isRead == null) {
            isRead = Boolean.FALSE;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(OffsetDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Long getShareLinkId() {
        return shareLinkId;
    }

    public void setShareLinkId(Long shareLinkId) {
        this.shareLinkId = shareLinkId;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getInviteType() {
        return inviteType;
    }

    public void setInviteType(String inviteType) {
        this.inviteType = inviteType;
    }

    public String getInviteStatus() {
        return inviteStatus;
    }

    public void setInviteStatus(String inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
