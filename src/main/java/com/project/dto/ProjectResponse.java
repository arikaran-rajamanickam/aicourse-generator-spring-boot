package com.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;

public class ProjectResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long creatorId;

    private OffsetDateTime createdAt;
    private List<CourseSummary> courses;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String name, String description,
                           Long creatorId, OffsetDateTime createdAt,
                           List<CourseSummary> courses) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.courses = courses;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CourseSummary> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseSummary> courses) {
        this.courses = courses;
    }

    // ─────────────────────────────────────────
    // Nested POJO — lightweight course summary
    // ─────────────────────────────────────────
    public static class CourseSummary {

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long id;

        private String title;
        private String description;
        private int moduleCount;

        public CourseSummary() {
        }

        public CourseSummary(Long id, String title, String description, int moduleCount) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.moduleCount = moduleCount;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String d) {
            this.description = d;
        }

        public int getModuleCount() {
            return moduleCount;
        }

        public void setModuleCount(int n) {
            this.moduleCount = n;
        }
    }
}