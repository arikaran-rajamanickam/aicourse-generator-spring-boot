package com.aicourse.repo;

import com.aicourse.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepo extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModuleTitle(String course);

    List<Lesson> findByModule_Id(Long moduleId);

    @Query(
            value = """
            SELECT *
            FROM lessons
            WHERE is_enriched = false
            ORDER BY created_at ASC
            LIMIT 2
        """,
            nativeQuery = true
    )
    List<Lesson> findNext2PendingLessons();

    @Query(
            value = """
            SELECT *
            FROM lessons
            WHERE is_enriched = false
            ORDER BY created_at ASC
            LIMIT :limit
        """,
            nativeQuery = true
    )
    List<Lesson> findNextPendingLessons(@Param("limit") int limit);

    @Query(
            value = """
            SELECT *
            FROM lessons
            WHERE module_id = :moduleId 
              AND is_enriched = false
            ORDER BY created_at ASC
            LIMIT :limit
        """,
            nativeQuery = true
    )
    List<Lesson> findPendingLessonsByModuleId(@Param("moduleId") Long moduleId, @Param("limit") int limit);

    @Query("""
                SELECT COUNT(l) FROM Lesson l
                WHERE l.module.course.id = :courseId
                AND l.isEnriched = false
            """)
    long countUnenrichedLessonsByCourseId(@Param("courseId") Long courseId);

    @Query("""
                SELECT COUNT(l) FROM Lesson l
                WHERE l.module.course.id = :courseId
            """)
    long countByCourseId(@Param("courseId") Long courseId);

    long countByIsEnriched(boolean isEnriched);
}
