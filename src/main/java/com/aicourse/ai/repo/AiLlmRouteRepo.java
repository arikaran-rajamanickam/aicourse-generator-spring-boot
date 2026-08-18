package com.aicourse.ai.repo;

import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.model.AiLlmRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiLlmRouteRepo extends JpaRepository<AiLlmRoute, Long> {
    Optional<AiLlmRoute> findByWorkload(AiWorkload workload);
}

