package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "course_progress_policies")
public class CourseProgressPolicy {

    @Id
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "min_lesson_seconds", nullable = false)
    private Long minLessonSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CourseProgressPolicy() {
    }

    public CourseProgressPolicy(Long courseId, Long minLessonSeconds) {
        this.id = SnowflakeIdGenerator.generateId();
        this.courseId = courseId;
        this.minLessonSeconds = minLessonSeconds;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (minLessonSeconds == null) {
            minLessonSeconds = 60L;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

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

    public Long getMinLessonSeconds() {
        return minLessonSeconds;
    }

    public void setMinLessonSeconds(Long minLessonSeconds) {
        this.minLessonSeconds = minLessonSeconds;
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

