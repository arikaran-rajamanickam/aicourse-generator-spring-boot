package com.project.repo;

import com.project.model.ProjectPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPromptRepo extends JpaRepository<ProjectPrompt, String> {
    List<ProjectPrompt> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    Optional<ProjectPrompt> findByProjectIdAndTextIgnoreCase(Long projectId, String text);
}
