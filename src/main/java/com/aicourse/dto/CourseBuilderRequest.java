package com.aicourse.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class CourseBuilderRequest {

    private String title;
    private String description;
    private String category;
    private JsonNode tags;
    private String difficulty;
    private EstimatedDuration estimatedDuration;
    private String thumbnailUrl;
    private List<ModuleRequest> modules;
    private JsonNode finalExam;
    private String status;
    private Settings settings;

    // Getters and Setters for top level
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public JsonNode getTags() {
        return tags;
    }

    public void setTags(JsonNode tags) {
        this.tags = tags;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public EstimatedDuration getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(EstimatedDuration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public List<ModuleRequest> getModules() {
        return modules;
    }

    public void setModules(List<ModuleRequest> modules) {
        this.modules = modules;
    }

    public JsonNode getFinalExam() {
        return finalExam;
    }

    public void setFinalExam(JsonNode finalExam) {
        this.finalExam = finalExam;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    // Inner classes
    public static class EstimatedDuration {
        private Integer value;
        private String unit;

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class Settings {
        private String visibility;
        private String enrollmentType;

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
    }

    public static class ModuleRequest {
        private String title;
        private String description;
        private JsonNode learningObjectives;
        private List<LessonRequest> lessons;
        private JsonNode assessment;
        private Integer order;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public JsonNode getLearningObjectives() {
            return learningObjectives;
        }

        public void setLearningObjectives(JsonNode learningObjectives) {
            this.learningObjectives = learningObjectives;
        }

        public List<LessonRequest> getLessons() {
            return lessons;
        }

        public void setLessons(List<LessonRequest> lessons) {
            this.lessons = lessons;
        }

        public JsonNode getAssessment() {
            return assessment;
        }

        public void setAssessment(JsonNode assessment) {
            this.assessment = assessment;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }
    }

    public static class LessonRequest {
        private String title;
        private JsonNode contentBlocks; // maps to Lesson.content
        private Integer order;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public JsonNode getContentBlocks() {
            return contentBlocks;
        }

        public void setContentBlocks(JsonNode contentBlocks) {
            this.contentBlocks = contentBlocks;
        }

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }
    }
}
