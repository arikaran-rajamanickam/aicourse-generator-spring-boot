package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lesson_id", "user_id"})
})
public class LessonProgress {

    @Id
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "progress_percentage")
    private Double progressPercentage;

    @Column(name = "total_time_seconds")
    private Long totalTimeSeconds;

    @Column(name = "last_session_started_at")
    private OffsetDateTime lastSessionStartedAt;

    @Column(name = "last_activity_at")
    private OffsetDateTime lastActivityAt;

    @Column(name = "completion_duration_seconds")
    private Long completionDurationSeconds;

    @Column(name = "completion_flagged")
    private Boolean completionFlagged;

    @Column(name = "completion_flag_reason")
    private String completionFlagReason;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // --- Constructors ---
    public LessonProgress() {
    }

    public LessonProgress(Long lessonId, Long userId, Long courseId) {
        this.id = SnowflakeIdGenerator.generateId();
        this.lessonId = lessonId;
        this.userId = userId;
        this.courseId = courseId;
        this.isCompleted = false;
        this.progressPercentage = 0.0;
        this.totalTimeSeconds = 0L;
        this.startedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.lastActivityAt = this.updatedAt;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (isCompleted == null) {
            isCompleted = false;
        }
        if (progressPercentage == null) {
            progressPercentage = 0.0;
        }
        if (totalTimeSeconds == null) {
            totalTimeSeconds = 0L;
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = updatedAt;
        }
        if (completionFlagged == null) {
            completionFlagged = Boolean.FALSE;
        }
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public OffsetDateTime getLastSessionStartedAt() {
        return lastSessionStartedAt;
    }

    public void setLastSessionStartedAt(OffsetDateTime lastSessionStartedAt) {
        this.lastSessionStartedAt = lastSessionStartedAt;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Long getCompletionDurationSeconds() {
        return completionDurationSeconds;
    }

    public void setCompletionDurationSeconds(Long completionDurationSeconds) {
        this.completionDurationSeconds = completionDurationSeconds;
    }

    public Boolean getCompletionFlagged() {
        return completionFlagged;
    }

    public void setCompletionFlagged(Boolean completionFlagged) {
        this.completionFlagged = completionFlagged;
    }

    public String getCompletionFlagReason() {
        return completionFlagReason;
    }

    public void setCompletionFlagReason(String completionFlagReason) {
        this.completionFlagReason = completionFlagReason;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
