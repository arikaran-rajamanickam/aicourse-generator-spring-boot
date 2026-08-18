package com.project.repo;

import com.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {

    List<Project> findByCreatorId(Long creatorId);

    @Query(
            value = "SELECT id FROM projects WHERE name = :name AND creator = :creator LIMIT 1",
            nativeQuery = true
    )
    Long isProjectAlreadyExist(String name, Long creator);

    int countByCreatorId(Long userId);
}