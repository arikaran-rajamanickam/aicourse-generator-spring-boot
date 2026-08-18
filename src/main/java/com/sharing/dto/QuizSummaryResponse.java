package com.sharing.dto;

public class QuizSummaryResponse {
    private int totalQuizAttempts;
    private int totalQuizzesAttempted;
    private int firstAttemptCorrect;
    private int retryCount;

    public QuizSummaryResponse() {
    }

    public QuizSummaryResponse(int totalQuizAttempts, int totalQuizzesAttempted, int firstAttemptCorrect, int retryCount) {
        this.totalQuizAttempts = totalQuizAttempts;
        this.totalQuizzesAttempted = totalQuizzesAttempted;
        this.firstAttemptCorrect = firstAttemptCorrect;
        this.retryCount = retryCount;
    }

    public int getTotalQuizAttempts() {
        return totalQuizAttempts;
    }

    public void setTotalQuizAttempts(int totalQuizAttempts) {
        this.totalQuizAttempts = totalQuizAttempts;
    }

    public int getTotalQuizzesAttempted() {
        return totalQuizzesAttempted;
    }

    public void setTotalQuizzesAttempted(int totalQuizzesAttempted) {
        this.totalQuizzesAttempted = totalQuizzesAttempted;
    }

    public int getFirstAttemptCorrect() {
        return firstAttemptCorrect;
    }

    public void setFirstAttemptCorrect(int firstAttemptCorrect) {
        this.firstAttemptCorrect = firstAttemptCorrect;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}

