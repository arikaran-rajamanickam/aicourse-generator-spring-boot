package com.aicourse.service.courses;

import com.aicourse.model.PrerequisiteExplanation;
import com.aicourse.repo.PrerequisiteExplanationRepo;
import com.aicourse.utils.id.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PrerequisiteExplanationService {

    private static final Logger LOGGER = Logger.getLogger(PrerequisiteExplanationService.class.getName());

    @Autowired
    private PrerequisiteExplanationRepo repo;

    /**
     * Look up a cached explanation for a given course + prerequisite + depth.
     */
    public Optional<PrerequisiteExplanation> findCached(Long courseId, String prerequisite, String depth) {
        return repo.findByCourseIdAndPrerequisiteAndDepth(courseId, prerequisite, depth);
    }

    /**
     * Store (or update) a prerequisite explanation.
     */
    @Transactional
    public PrerequisiteExplanation save(Long courseId, String prerequisite, String depth, JsonNode responseData) {
        Optional<PrerequisiteExplanation> existing = repo.findByCourseIdAndPrerequisiteAndDepth(courseId, prerequisite, depth);

        if (existing.isPresent()) {
            PrerequisiteExplanation entity = existing.get();
            entity.setResponseData(responseData);
            LOGGER.log(Level.FINE, "Updated existing prerequisite explanation for: {0}", prerequisite);
            return repo.save(entity);
        }

        PrerequisiteExplanation entity = new PrerequisiteExplanation();
        entity.setId(SnowflakeIdGenerator.generateId());
        entity.setCourseId(courseId);
        entity.setPrerequisite(prerequisite);
        entity.setDepth(depth);
        entity.setResponseData(responseData);
        LOGGER.log(Level.FINE, "Saved new prerequisite explanation for: {0}", prerequisite);
        return repo.save(entity);
    }

    /**
     * Get all cached explanations for a course.
     */
    public List<PrerequisiteExplanation> findAllForCourse(Long courseId) {
        return repo.findByCourseId(courseId);
    }
}

