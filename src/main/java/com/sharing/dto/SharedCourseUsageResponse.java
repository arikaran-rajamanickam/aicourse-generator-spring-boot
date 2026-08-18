package com.sharing.dto;

import java.util.List;

public class SharedCourseUsageResponse {
    private Long courseId;
    private Long userId;
    private String courseTitle;
    private int totalModules;
    private int completedModules;
    private int totalLessons;
    private int completedLessons;
    private long totalTimeSeconds;
    private List<ModuleUsageResponse> modules;
    private List<LessonUsageResponse> lessons;
    private List<LessonUsageResponse> pendingLessons;
    private QuizSummaryResponse quizSummary;

    public SharedCourseUsageResponse() {
    }

    public SharedCourseUsageResponse(Long courseId, Long userId, String courseTitle,
                                     int totalModules, int completedModules, int totalLessons, int completedLessons,
                                     long totalTimeSeconds, List<ModuleUsageResponse> modules,
                                     List<LessonUsageResponse> lessons, List<LessonUsageResponse> pendingLessons,
                                     QuizSummaryResponse quizSummary) {
        this.courseId = courseId;
        this.userId = userId;
        this.courseTitle = courseTitle;
        this.totalModules = totalModules;
        this.completedModules = completedModules;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.totalTimeSeconds = totalTimeSeconds;
        this.modules = modules;
        this.lessons = lessons;
        this.pendingLessons = pendingLessons;
        this.quizSummary = quizSummary;
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

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public int getTotalModules() {
        return totalModules;
    }

    public void setTotalModules(int totalModules) {
        this.totalModules = totalModules;
    }

    public int getCompletedModules() {
        return completedModules;
    }

    public void setCompletedModules(int completedModules) {
        this.completedModules = completedModules;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public List<ModuleUsageResponse> getModules() {
        return modules;
    }

    public void setModules(List<ModuleUsageResponse> modules) {
        this.modules = modules;
    }

    public List<LessonUsageResponse> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonUsageResponse> lessons) {
        this.lessons = lessons;
    }

    public List<LessonUsageResponse> getPendingLessons() {
        return pendingLessons;
    }

    public void setPendingLessons(List<LessonUsageResponse> pendingLessons) {
        this.pendingLessons = pendingLessons;
    }

    public QuizSummaryResponse getQuizSummary() {
        return quizSummary;
    }

    public void setQuizSummary(QuizSummaryResponse quizSummary) {
        this.quizSummary = quizSummary;
    }
}

