package com.sharing.repo;

import com.sharing.model.LessonSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LessonSessionRepo extends JpaRepository<LessonSession, Long> {
    @Query(value = "select * from lesson_sessions ls where ls.lesson_id = :lessonId and ls.user_id = :userId and ls.ended_at is null order by ls.started_at desc limit 1", nativeQuery = true)
    Optional<LessonSession> findTopByLessonIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(@Param("lessonId") Long lessonId,
                                                                                           @Param("userId") Long userId);

    @Query("select coalesce(sum(ls.durationSeconds), 0) from LessonSession ls where ls.lessonId = :lessonId and ls.userId = :userId")
    Long sumDurationSecondsForLessonUser(@Param("lessonId") Long lessonId, @Param("userId") Long userId);

    @Query("select coalesce(sum(ls.durationSeconds), 0) from LessonSession ls where ls.courseId = :courseId and ls.userId = :userId")
    Long sumDurationSecondsForCourseUser(@Param("courseId") Long courseId, @Param("userId") Long userId);
}

