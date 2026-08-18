package com.aicourse.repo;

import com.aicourse.model.PrerequisiteExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrerequisiteExplanationRepo extends JpaRepository<PrerequisiteExplanation, Long> {

    Optional<PrerequisiteExplanation> findByCourseIdAndPrerequisiteAndDepth(
            Long courseId, String prerequisite, String depth);

    List<PrerequisiteExplanation> findByCourseId(Long courseId);
}

