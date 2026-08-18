package com.aicourse.ai.repo;

import com.aicourse.ai.model.AiLlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiLlmProviderRepo extends JpaRepository<AiLlmProvider, Long> {
    Optional<AiLlmProvider> findByCodeIgnoreCase(String code);
}

