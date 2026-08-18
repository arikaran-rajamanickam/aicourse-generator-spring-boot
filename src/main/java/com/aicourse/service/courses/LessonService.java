package com.aicourse.service.courses;

import com.aicourse.model.Lesson;

public interface LessonService {

    Lesson generateLessonContent(Long courseId, Long moduleId, Long lessonId, Long userId) throws Exception;

    java.util.List<com.aicourse.dto.GenerationLog> enrichPendingLessonsLimited() throws Exception;

    java.util.List<com.aicourse.dto.GenerationLog> enrichPendingLessonsLimited(int batchSize) throws Exception;

    Lesson getLesson(Long lessonId) throws Exception;

    java.util.List<Lesson> batchGenerateLessonsForModule(Long courseId, Long moduleId, int limit, Long userId) throws Exception;

}
