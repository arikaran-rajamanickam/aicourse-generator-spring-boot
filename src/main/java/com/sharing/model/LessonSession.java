package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lesson_sessions")
public class LessonSession {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public LessonSession() {
    }

    public LessonSession(Long lessonId, Long courseId, Long userId, OffsetDateTime startedAt) {
        this.id = SnowflakeIdGenerator.generateId();
        this.lessonId = lessonId;
        this.courseId = courseId;
        this.userId = userId;
        this.startedAt = startedAt;
        this.createdAt = startedAt;
        this.updatedAt = startedAt;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (createdAt == null) {
            createdAt = startedAt;
        }
        if (updatedAt == null) {
            updatedAt = startedAt;
        }
    }

    public long close(OffsetDateTime endTime) {
        if (endTime == null) {
            endTime = OffsetDateTime.now();
        }
        this.endedAt = endTime;
        long duration = Duration.between(startedAt, endTime).getSeconds();
        this.durationSeconds = Math.max(0L, duration);
        this.updatedAt = endTime;
        return this.durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
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

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

