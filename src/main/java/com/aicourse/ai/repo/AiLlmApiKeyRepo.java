package com.aicourse.ai.repo;

import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiLlmApiKeyRepo extends JpaRepository<AiLlmApiKey, Long> {
    List<AiLlmApiKey> findByProviderAndEnabledTrueOrderByIdAsc(AiLlmProvider provider);

    void deleteByProvider(AiLlmProvider provider);
}

