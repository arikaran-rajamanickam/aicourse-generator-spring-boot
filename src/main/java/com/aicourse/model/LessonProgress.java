package com.aicourse.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity(name = "AiCourseLessonProgress")
@Table(name = "lesson_progress")
public class LessonProgress {

    @EmbeddedId
    private LessonProgressId id;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    public LessonProgress() {
    }

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) {
            completedAt = OffsetDateTime.now();
        }
    }

    public LessonProgressId getId() {
        return id;
    }

    public void setId(LessonProgressId id) {
        this.id = id;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
