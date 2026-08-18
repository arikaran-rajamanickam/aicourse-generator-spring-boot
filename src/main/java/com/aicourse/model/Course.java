package com.aicourse.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course implements Persistable<Long> {

    @Id
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", length = 255)
    private String source = "ai";

    @Column(columnDefinition = "TEXT")
    private String topic;

    // Auth0 sub
    @Column(nullable = false)
    private Long creator;

    @OneToMany(
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Module> modules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "category")
    private String category;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "estimated_duration_value")
    private Integer estimatedDurationValue;

    @Column(name = "estimated_duration_unit")
    private String estimatedDurationUnit;

    @Column(name = "thumbnail_url", length = 1500)
    private String thumbnailUrl;

    @Column(name = "visibility")
    private String visibility = "PRIVATE";

    @Column(name = "enrollment_type")
    private String enrollmentType = "open";

    @Column(name = "tags_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode tagsJson;

    @Column(name = "final_exam", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode finalExam;

    @Column(name = "overview", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode overview;

    @Column(name = "capstone_project", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode capstoneProject;

    // --- Persistable ---
    @Override
    public Long getId() {
        return id;
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
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (visibility == null || visibility.isBlank()) {
            visibility = "PRIVATE";
        }
        if (enrollmentType == null || enrollmentType.isBlank()) {
            enrollmentType = "open";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters & Setters ---
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }
    public List<Module> getModules() { return modules; }
    public void setModules(List<Module> modules) { this.modules = modules; }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getEstimatedDurationValue() {
        return estimatedDurationValue;
    }

    public void setEstimatedDurationValue(Integer estimatedDurationValue) {
        this.estimatedDurationValue = estimatedDurationValue;
    }

    public String getEstimatedDurationUnit() {
        return estimatedDurationUnit;
    }

    public void setEstimatedDurationUnit(String estimatedDurationUnit) {
        this.estimatedDurationUnit = estimatedDurationUnit;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getEnrollmentType() {
        return enrollmentType;
    }

    public void setEnrollmentType(String enrollmentType) {
        this.enrollmentType = enrollmentType;
    }

    public JsonNode getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(JsonNode tagsJson) {
        this.tagsJson = tagsJson;
    }

    public JsonNode getFinalExam() {
        return finalExam;
    }

    public void setFinalExam(JsonNode finalExam) {
        this.finalExam = finalExam;
    }

    public JsonNode getOverview() {
        return overview;
    }

    public void setOverview(JsonNode overview) {
        this.overview = overview;
    }

    public JsonNode getCapstoneProject() {
        return capstoneProject;
    }

    public void setCapstoneProject(JsonNode capstoneProject) {
        this.capstoneProject = capstoneProject;
    }
}
