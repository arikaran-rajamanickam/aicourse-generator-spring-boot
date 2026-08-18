package com.aicourse.repo;

import com.aicourse.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepo extends JpaRepository<Course, Long> {
    List<Course> findByCreator(Long creator);

    List<Course> findByProjectId(Long projectId);

    int countByCreator(Long userId);
}