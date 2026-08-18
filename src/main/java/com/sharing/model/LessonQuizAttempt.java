package com.sharing.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lesson_quiz_attempts")
public class LessonQuizAttempt {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "quiz_index", nullable = false)
    private Integer quizIndex;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "is_correct", nullable = false)
    private Boolean correct;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    public LessonQuizAttempt() {
    }

    public LessonQuizAttempt(Long lessonId, Long courseId, Long userId, Integer quizIndex, Integer attemptNumber, Boolean correct) {
        this.id = SnowflakeIdGenerator.generateId();
        this.lessonId = lessonId;
        this.courseId = courseId;
        this.userId = userId;
        this.quizIndex = quizIndex;
        this.attemptNumber = attemptNumber;
        this.correct = correct;
        this.attemptedAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = SnowflakeIdGenerator.generateId();
        }
        if (attemptedAt == null) {
            attemptedAt = OffsetDateTime.now();
        }
        if (correct == null) {
            correct = Boolean.FALSE;
        }
    }

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

    public Integer getQuizIndex() {
        return quizIndex;
    }

    public void setQuizIndex(Integer quizIndex) {
        this.quizIndex = quizIndex;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public OffsetDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(OffsetDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}

