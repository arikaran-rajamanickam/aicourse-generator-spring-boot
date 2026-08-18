package com.sharing.dto;

public class CourseLeaderboardEntry {
    private Long userId;
    private String username;
    private String userHandle;
    private Integer rank;
    private Double score;          // composite score 0–1000
    private Double totalProgress;  // lesson completion % (0–100)
    private Integer lessonsCompleted;
    private Double quizAccuracy;   // first-attempt correct % (0–100)
    private Long totalTimeSeconds;
    private Integer flaggedCount;  // lessons flagged for fast completion

    // --- Constructors ---
    public CourseLeaderboardEntry() {
    }

    public CourseLeaderboardEntry(Long userId, String username, String userHandle, Integer rank,
                                  Double score, Double totalProgress,
                                  Integer lessonsCompleted, Double quizAccuracy,
                                  Long totalTimeSeconds, Integer flaggedCount) {
        this.userId = userId;
        this.username = username;
        this.userHandle = userHandle;
        this.rank = rank;
        this.score = score;
        this.totalProgress = totalProgress;
        this.lessonsCompleted = lessonsCompleted;
        this.quizAccuracy = quizAccuracy;
        this.totalTimeSeconds = totalTimeSeconds;
        this.flaggedCount = flaggedCount;
    }

    // --- Getters and Setters ---
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getTotalProgress() {
        return totalProgress;
    }

    public void setTotalProgress(Double totalProgress) {
        this.totalProgress = totalProgress;
    }

    public Integer getLessonsCompleted() {
        return lessonsCompleted;
    }

    public void setLessonsCompleted(Integer lessonsCompleted) {
        this.lessonsCompleted = lessonsCompleted;
    }

    public Double getQuizAccuracy() {
        return quizAccuracy;
    }

    public void setQuizAccuracy(Double quizAccuracy) {
        this.quizAccuracy = quizAccuracy;
    }

    public Long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public Integer getFlaggedCount() {
        return flaggedCount;
    }

    public void setFlaggedCount(Integer flaggedCount) {
        this.flaggedCount = flaggedCount;
    }
}
