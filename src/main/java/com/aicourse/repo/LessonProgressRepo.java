package com.aicourse.repo;

import com.aicourse.model.LessonProgress;
import com.aicourse.model.LessonProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("aiCourseLessonProgressRepo")
public interface LessonProgressRepo extends JpaRepository<LessonProgress, LessonProgressId> {
    List<LessonProgress> findByIdUserId(Long userId);
}
