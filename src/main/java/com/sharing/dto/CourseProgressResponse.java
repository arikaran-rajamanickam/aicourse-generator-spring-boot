package com.sharing.dto;

import java.time.OffsetDateTime;

public class CourseProgressResponse {
    private Long courseId;
    private String courseName;
    private String courseDescription;
    private Double courseProgress;
    private Integer totalLessons;
    private Integer completedLessons;
    private OffsetDateTime enrolledAt;
    private OffsetDateTime lastAccessedAt;
    private Boolean contentLocked;
    private String lockReason;

    // --- Constructors ---
    public CourseProgressResponse() {
    }

    public CourseProgressResponse(Long courseId, String courseName, String courseDescription, Double courseProgress,
                                  Integer totalLessons, Integer completedLessons,
                                  OffsetDateTime enrolledAt, OffsetDateTime lastAccessedAt) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.courseProgress = courseProgress;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.enrolledAt = enrolledAt;
        this.lastAccessedAt = lastAccessedAt;
    }

    public CourseProgressResponse(Long courseId, String courseName, String courseDescription, Double courseProgress,
                                  Integer totalLessons, Integer completedLessons,
                                  OffsetDateTime enrolledAt, OffsetDateTime lastAccessedAt,
                                  Boolean contentLocked, String lockReason) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.courseProgress = courseProgress;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.enrolledAt = enrolledAt;
        this.lastAccessedAt = lastAccessedAt;
        this.contentLocked = contentLocked;
        this.lockReason = lockReason;
    }

    // --- Getters and Setters ---
    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public Double getCourseProgress() {
        return courseProgress;
    }

    public void setCourseProgress(Double courseProgress) {
        this.courseProgress = courseProgress;
    }

    public Integer getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(Integer totalLessons) {
        this.totalLessons = totalLessons;
    }

    public Integer getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(Integer completedLessons) {
        this.completedLessons = completedLessons;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(OffsetDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public OffsetDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(OffsetDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Boolean getContentLocked() {
        return contentLocked;
    }

    public void setContentLocked(Boolean contentLocked) {
        this.contentLocked = contentLocked;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }
}

