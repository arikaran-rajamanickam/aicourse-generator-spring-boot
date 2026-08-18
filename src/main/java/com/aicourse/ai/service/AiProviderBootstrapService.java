package com.aicourse.ai.service;

import com.aicourse.ai.AiProviderType;
import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.model.AiLlmRoute;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmProviderRepo;
import com.aicourse.ai.repo.AiLlmRouteRepo;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiProviderBootstrapService {

    private final AiLlmProviderRepo providerRepo;
    private final AiLlmApiKeyRepo apiKeyRepo;
    private final AiLlmRouteRepo routeRepo;
    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${spring.ai.groq.chat.options.model:llama3-8b-8192}")
    private String groqModel;

    public AiProviderBootstrapService(AiLlmProviderRepo providerRepo,
                                      AiLlmApiKeyRepo apiKeyRepo,
                                      AiLlmRouteRepo routeRepo) {
        this.providerRepo = providerRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.routeRepo = routeRepo;
    }

    @PostConstruct
    @Transactional
    public void initialize() {
        AiLlmProvider gemini = ensureProvider(
                "gemini",
                "Google Gemini",
                AiProviderType.GEMINI,
                geminiModel,
                null,
                24
        );

        AiLlmProvider groq = ensureProvider(
                "groq",
                "Groq",
                AiProviderType.GROQ,
                groqModel,
                "https://api.groq.com/openai/v1/chat/completions",
                24
        );

        ensureRoute(AiWorkload.COURSE_GENERATION, gemini);
        ensureRoute(AiWorkload.LESSON_GENERATION, gemini);
        ensureRoute(AiWorkload.AI_COACH, gemini);
    }

    private AiLlmProvider ensureProvider(String code,
                                         String displayName,
                                         AiProviderType providerType,
                                         String model,
                                         String baseUrl,
                                         int cooldownHours) {
        var existing = providerRepo.findByCodeIgnoreCase(code);
        if (existing.isPresent()) {
            return existing.get();
        }

        AiLlmProvider provider = new AiLlmProvider();
        provider.setCode(code);
        provider.setDisplayName(displayName);
        provider.setProviderType(providerType);
        provider.setModelName(model);
        provider.setBaseUrl(baseUrl);
        provider.setEnabled(true);
        provider.setKeyCooldownHours(cooldownHours);
        return providerRepo.save(provider);
    }



    private void ensureRoute(AiWorkload workload, AiLlmProvider provider) {
        if (routeRepo.findByWorkload(workload).isPresent()) {
            return;
        }
        AiLlmRoute route = new AiLlmRoute();
        route.setWorkload(workload);
        route.setProvider(provider);
        routeRepo.save(route);
    }
}


