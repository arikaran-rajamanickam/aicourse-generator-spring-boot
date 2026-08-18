package com.sharing.dto;

public class ModuleUsageResponse {
    private Long moduleId;
    private String moduleTitle;
    private int totalLessons;
    private int completedLessons;
    private double completionPercentage;

    public ModuleUsageResponse() {
    }

    public ModuleUsageResponse(Long moduleId, String moduleTitle, int totalLessons, int completedLessons, double completionPercentage) {
        this.moduleId = moduleId;
        this.moduleTitle = moduleTitle;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.completionPercentage = completionPercentage;
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

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}

