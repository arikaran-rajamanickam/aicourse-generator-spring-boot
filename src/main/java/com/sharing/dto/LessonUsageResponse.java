package com.sharing.dto;

import java.time.OffsetDateTime;

public class LessonUsageResponse {
    private Long lessonId;
    private String lessonTitle;
    private Long moduleId;
    private String moduleTitle;
    private boolean completed;
    private OffsetDateTime completedAt;
    private Long timeSpentSeconds;
    private Long completionDurationSeconds;
    private Boolean completionFlagged;
    private String completionFlagReason;
    private int quizAttempts;
    private int quizFirstAttemptCorrect;
    private int quizRetryCount;

    public LessonUsageResponse() {
    }

    public LessonUsageResponse(Long lessonId, String lessonTitle, Long moduleId, String moduleTitle,
                               boolean completed, OffsetDateTime completedAt, Long timeSpentSeconds,
                               Long completionDurationSeconds, Boolean completionFlagged, String completionFlagReason,
                               int quizAttempts, int quizFirstAttemptCorrect, int quizRetryCount) {
        this.lessonId = lessonId;
        this.lessonTitle = lessonTitle;
        this.moduleId = moduleId;
        this.moduleTitle = moduleTitle;
        this.completed = completed;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
        this.completionDurationSeconds = completionDurationSeconds;
        this.completionFlagged = completionFlagged;
        this.completionFlagReason = completionFlagReason;
        this.quizAttempts = quizAttempts;
        this.quizFirstAttemptCorrect = quizFirstAttemptCorrect;
        this.quizRetryCount = quizRetryCount;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleTitle() {
        return moduleTitle;
    }

    public void setModuleTitle(String moduleTitle) {
        this.moduleTitle = moduleTitle;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Long timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
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

    public int getQuizAttempts() {
        return quizAttempts;
    }

    public void setQuizAttempts(int quizAttempts) {
        this.quizAttempts = quizAttempts;
    }

    public int getQuizFirstAttemptCorrect() {
        return quizFirstAttemptCorrect;
    }

    public void setQuizFirstAttemptCorrect(int quizFirstAttemptCorrect) {
        this.quizFirstAttemptCorrect = quizFirstAttemptCorrect;
    }

    public int getQuizRetryCount() {
        return quizRetryCount;
    }

    public void setQuizRetryCount(int quizRetryCount) {
        this.quizRetryCount = quizRetryCount;
    }
}

