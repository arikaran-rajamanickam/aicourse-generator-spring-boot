package com.sharing.repo;

import com.sharing.model.LessonQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonQuizAttemptRepo extends JpaRepository<LessonQuizAttempt, Long> {
    List<LessonQuizAttempt> findByCourseIdAndUserId(Long courseId, Long userId);

    List<LessonQuizAttempt> findByCourseId(Long courseId);

    List<LessonQuizAttempt> findByLessonIdAndUserId(Long lessonId, Long userId);

    @Query("select coalesce(max(lqa.attemptNumber), 0) from LessonQuizAttempt lqa where lqa.lessonId = :lessonId and lqa.userId = :userId and lqa.quizIndex = :quizIndex")
    Integer findMaxAttemptNumber(@Param("lessonId") Long lessonId,
                                 @Param("userId") Long userId,
                                 @Param("quizIndex") Integer quizIndex);
}

