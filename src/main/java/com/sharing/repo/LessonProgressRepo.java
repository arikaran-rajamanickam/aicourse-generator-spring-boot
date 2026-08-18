package com.sharing.repo;

import com.sharing.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepo extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByLessonIdAndUserId(Long lessonId, Long userId);

    List<LessonProgress> findByUserId(Long userId);

    List<LessonProgress> findByCourseId(Long courseId);

    List<LessonProgress> findByUserIdAndCourseId(Long userId, Long courseId);

    List<LessonProgress> findByUserIdAndIsCompletedTrue(Long userId);

    int countByUserIdAndCourseIdAndIsCompletedTrue(Long userId, Long courseId);

    int countByUserIdAndIsCompletedTrue(Long userId);

    int countByCourseIdAndIsCompletedTrue(Long courseId);

    Optional<LessonProgress> findTopByUserIdAndCourseIdOrderByLastActivityAtDesc(Long userId, Long courseId);
}
