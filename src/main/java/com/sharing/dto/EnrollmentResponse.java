package com.sharing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sharing.model.EnrollmentStatus;

import java.time.OffsetDateTime;

public class EnrollmentResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long courseId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    private EnrollmentStatus status;
    private OffsetDateTime enrolledAt;
    private Double progressPercentage;
    private String courseName;
    private String courseDescription;
    private Boolean isRead;
    private String inviteStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long invitedBy;

    private String invitedByName;
    private int moduleCount;
    private int lessonCount;
    private String userName;
    private String userHandle;

    // --- Constructors ---
    public EnrollmentResponse() {
    }

    public EnrollmentResponse(Long id, Long courseId, Long userId, EnrollmentStatus status,
                              OffsetDateTime enrolledAt, Double progressPercentage, String courseName) {
        this.id = id;
        this.courseId = courseId;
        this.userId = userId;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.progressPercentage = progressPercentage;
        this.courseName = courseName;
    }

    public EnrollmentResponse(Long id, Long courseId, Long userId, EnrollmentStatus status,
                              OffsetDateTime enrolledAt, Double progressPercentage, String courseName,
                              String courseDescription, Boolean isRead, String inviteStatus,
                              Long invitedBy, String invitedByName, int moduleCount, int lessonCount, String userName, String userHandle) {
        this.id = id;
        this.courseId = courseId;
        this.userId = userId;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.progressPercentage = progressPercentage;
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.isRead = isRead;
        this.inviteStatus = inviteStatus;
        this.invitedBy = invitedBy;
        this.invitedByName = invitedByName;
        this.moduleCount = moduleCount;
        this.lessonCount = lessonCount;
        this.userName = userName;
        this.userHandle = userHandle;
    }

    // --- Getters and Setters ---
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(OffsetDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getInviteStatus() {
        return inviteStatus;
    }

    public void setInviteStatus(String inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getInvitedByName() {
        return invitedByName;
    }

    public void setInvitedByName(String invitedByName) {
        this.invitedByName = invitedByName;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    public void setModuleCount(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = lessonCount;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }
}
