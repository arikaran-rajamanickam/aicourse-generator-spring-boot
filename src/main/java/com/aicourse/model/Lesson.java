package com.aicourse.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lessons")
public class Lesson implements Persistable<Long> {

    @Id
    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Column(nullable = false)
    private String title;

    // Flexible structured blocks
    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)

    private JsonNode content;

    @Column(name = "content_md", columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "is_enriched", nullable = false)
    private boolean isEnriched = false;

    @ManyToOne
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Lesson parentLesson;

    @OneToMany(mappedBy = "parentLesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Lesson> subLessons;

    @Column(name = "sort_order", nullable = false)
    private Integer order = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public Long getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    private void markNotNew() { this.isNew = false; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters & Setters
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public JsonNode getContent() { return content; }
    public void setContent(JsonNode content) { this.content = content; }

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
    public boolean isEnriched() { return isEnriched; }
    public void setEnriched(boolean enriched) { isEnriched = enriched; }
    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Lesson getParentLesson() {
        return parentLesson;
    }

    public void setParentLesson(Lesson parentLesson) {
        this.parentLesson = parentLesson;
    }

    public java.util.List<Lesson> getSubLessons() {
        return subLessons;
    }

    public void setSubLessons(java.util.List<Lesson> subLessons) {
        this.subLessons = subLessons;
    }
}
