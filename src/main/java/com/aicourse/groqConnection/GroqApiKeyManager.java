package com.aicourse.groqConnection;

import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmProviderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
public class GroqApiKeyManager {

    private static final List<String> QUOTA_ERROR_TOKENS = List.of(
            "resource_exhausted",
            "quota",
            "rate limit",
            "rate_limit",
            "too many requests",
            "status code 429",
            "http 429"
    );

    private final AiLlmProviderRepo providerRepo;
    private final AiLlmApiKeyRepo apiKeyRepo;
    private final Clock clock;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final Map<String, Instant> keyCooldownUntil = new ConcurrentHashMap<>();

    @Autowired
    public GroqApiKeyManager(AiLlmProviderRepo providerRepo, AiLlmApiKeyRepo apiKeyRepo) {
        this(providerRepo, apiKeyRepo, Clock.systemUTC());
    }

    // Secondary constructor used by unit tests.
    GroqApiKeyManager(AiLlmProviderRepo providerRepo, AiLlmApiKeyRepo apiKeyRepo, Clock clock) {
        this.providerRepo = providerRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public synchronized String acquireAvailableKey() {
        Instant now = clock.instant();

        AiLlmProvider provider = providerRepo.findByCodeIgnoreCase("groq")
                .orElseThrow(() -> new IllegalStateException("Groq provider not found in database."));

        List<AiLlmApiKey> dbKeys = apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider);
        List<String> currentKeys = dbKeys.stream().map(AiLlmApiKey::getApiKey).toList();

        if (currentKeys.isEmpty()) {
            throw new IllegalStateException("No Groq API keys configured in the database.");
        }

        Duration cooldownDuration = Duration.ofHours(Math.max(1, provider.getKeyCooldownHours()));

        for (int attempt = 0; attempt < currentKeys.size(); attempt++) {
            int index = Math.floorMod(roundRobinIndex.getAndIncrement(), currentKeys.size());
            String candidate = currentKeys.get(index);
            Instant cooldownUntil = keyCooldownUntil.get(candidate);

            if (cooldownUntil == null || !cooldownUntil.isAfter(now)) {
                keyCooldownUntil.remove(candidate);
                return candidate;
            }
        }

        Instant earliestReadyAt = keyCooldownUntil.values().stream()
                .min(Comparator.naturalOrder())
                .orElse(now.plus(cooldownDuration));
        long waitSeconds = Math.max(1, Duration.between(now, earliestReadyAt).getSeconds());

        throw new IllegalStateException("All Groq API keys are on cooldown. Retry in about " + waitSeconds + " seconds.");
    }

    public void markKeyOnCooldown(String key) {
        int cooldownHours = providerRepo.findByCodeIgnoreCase("groq")
                .map(AiLlmProvider::getKeyCooldownHours)
                .orElse(24);
        keyCooldownUntil.put(key, clock.instant().plus(Duration.ofHours(Math.max(1, cooldownHours))));
    }

    public boolean isQuotaOrRateLimitError(Throwable error) {
        String combinedMessage = buildErrorMessage(error).toLowerCase(Locale.ROOT);
        return QUOTA_ERROR_TOKENS.stream().anyMatch(combinedMessage::contains);
    }

    public int totalKeyCount() {
        return providerRepo.findByCodeIgnoreCase("groq")
                .map(provider -> apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider).size())
                .orElse(0);
    }

    private String buildErrorMessage(Throwable error) {
        StringBuilder message = new StringBuilder();
        Throwable cursor = error;

        while (cursor != null) {
            if (cursor.getMessage() != null) {
                message.append(cursor.getMessage()).append(' ');
            }
            cursor = cursor.getCause();
        }

        return message.toString();
    }
}

