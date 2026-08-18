package com.project.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_prompts")
public class ProjectPrompt implements Persistable<String> {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "project_id", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "related_course_id")
    private String relatedCourseId;

    @Column(name = "related_course_title")
    private String relatedCourseTitle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
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
