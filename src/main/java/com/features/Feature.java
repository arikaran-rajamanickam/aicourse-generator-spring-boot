package com.features;

public enum Feature {

    // Course features
    COURSE_CREATE,
    COURSE_DELETE,
    COURSE_RENAME,

    // Project features
    PROJECT_CREATE,

    // Lesson features
    LESSON_GENERATE,

    // Study assistant features
    AI_COACH,

    // Premium-only features
    ADVANCED_COURSE_SETTINGS,  // custom quiz count, file upload, etc. (future)
    API_KEY_COURSE_GENERATION, // user provides own API key (future)

    // Admin-only features
    ADMIN_PANEL,
    MANAGE_USERS,
    VIEW_ALL_COURSES
}