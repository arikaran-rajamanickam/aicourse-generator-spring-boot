package com.aicourse.dto;

public record GenerationLog(
    Long lessonId,
    String lessonTitle,
    Long moduleId,
    String moduleTitle,
    Long courseId,
    String courseTitle,
    boolean success,
    String errorMessage,
    long timestampMs
) {}
