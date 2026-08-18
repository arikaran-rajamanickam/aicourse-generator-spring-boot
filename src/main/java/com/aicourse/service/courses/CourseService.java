package com.aicourse.service.courses;

import com.aicourse.dto.CourseBuilderRequest;
import com.aicourse.model.Course;
import com.aicourse.model.Lesson;
import com.aicourse.model.Module;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface CourseService {

    Course saveBuiltCourse(CourseBuilderRequest payload, Authentication auth) throws Exception;

    JsonNode generateCourseOutlineOnly(Map<String, String> payload, Authentication auth) throws Exception;

    Course generateCourse(Map<String, String> payload, Authentication auth) throws Exception;

    List<Course> getCoursesByCreator(Long creator) throws Exception;

    List<Course> getCoursesSharedByCreator(Long creator) throws Exception;

    Course getCourseById(Long id, Long requesterId) throws Exception;

    List<Module> getModulesByCourseName(String courseName) throws Exception;

    void deleteCourse(Long courseId) throws Exception;

    void updateCourse(Long courseID, Course courseDO) throws Exception;

    void deactivateCourse(Long courseId) throws Exception;

    void activateCourse(Long courseId) throws Exception;

    Module addModule(Long courseId, String title) throws Exception;

    void renameModule(Long moduleId, String title) throws Exception;

    void deleteModule(Long moduleId) throws Exception;

    Lesson addLesson(Long moduleId, String title) throws Exception;

    void renameLesson(Long lessonId, String title) throws Exception;

    void deleteLesson(Long lessonId) throws Exception;

}