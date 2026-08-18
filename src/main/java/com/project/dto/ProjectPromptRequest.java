package com.project.dto;

public class ProjectPromptRequest {
    private String text;
    private String relatedCourseId;
    private String relatedCourseTitle;

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
}
