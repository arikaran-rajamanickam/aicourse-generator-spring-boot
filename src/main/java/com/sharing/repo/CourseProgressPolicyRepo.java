package com.sharing.repo;

import com.sharing.model.CourseProgressPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseProgressPolicyRepo extends JpaRepository<CourseProgressPolicy, Long> {
    Optional<CourseProgressPolicy> findByCourseId(Long courseId);
}

