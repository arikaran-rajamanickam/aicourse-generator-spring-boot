package com.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public class ProjectPromptResponse {
    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    private String text;
    private String relatedCourseId;
    private String relatedCourseTitle;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastUsedAt;

    public ProjectPromptResponse() {
    }

    public ProjectPromptResponse(String id, Long projectId, String text, String relatedCourseId, String relatedCourseTitle, OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime lastUsedAt) {
        this.id = id;
        this.projectId = projectId;
        this.text = text;
        this.relatedCourseId = relatedCourseId;
        this.relatedCourseTitle = relatedCourseTitle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastUsedAt = lastUsedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRelatedCourseId() {
        return relatedCourseId;
    }

    public void setRelatedCourseId(String relatedCourseId) {
        this.relatedCourseId = relatedCourseId;
    }

    public String getRelatedCourseTitle() {
        return relatedCourseTitle;
    }

    public void setRelatedCourseTitle(String relatedCourseTitle) {
        this.relatedCourseTitle = relatedCourseTitle;
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

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
