package com.aicourse.repo;

import com.aicourse.model.LessonMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonMediaRepo extends JpaRepository<LessonMedia, Long> {
    List<LessonMedia> findByLessonId(Long lessonId);
}
