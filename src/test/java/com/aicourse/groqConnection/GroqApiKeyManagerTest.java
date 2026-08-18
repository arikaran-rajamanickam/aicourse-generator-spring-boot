package com.aicourse.groqConnection;

import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmProviderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class GroqApiKeyManagerTest {

    @Mock
    private AiLlmProviderRepo providerRepo;

    @Mock
    private AiLlmApiKeyRepo apiKeyRepo;

    private AiLlmProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AiLlmProvider();
        provider.setCode("groq");
        provider.setKeyCooldownHours(24);
        
        Mockito.lenient().when(providerRepo.findByCodeIgnoreCase("groq")).thenReturn(Optional.of(provider));
    }

    private void mockKeys(List<String> keys) {
        List<AiLlmApiKey> dbKeys = keys.stream().map(keyStr -> {
            AiLlmApiKey key = new AiLlmApiKey();
            key.setApiKey(keyStr);
            key.setEnabled(true);
            return key;
        }).toList();
        Mockito.lenient().when(apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(any())).thenReturn(dbKeys);
    }

    @Test
    void acquireAvailableKeyRotatesInRoundRobinOrder() {
        mockKeys(List.of("k1", "k2", "k3"));
        GroqApiKeyManager manager = new GroqApiKeyManager(providerRepo, apiKeyRepo, Clock.systemUTC());

        assertEquals("k1", manager.acquireAvailableKey());
        assertEquals("k2", manager.acquireAvailableKey());
        assertEquals("k3", manager.acquireAvailableKey());
        assertEquals("k1", manager.acquireAvailableKey());
    }

    @Test
    void acquireAvailableKeySkipsCoolingKey() {
        mockKeys(List.of("k1", "k2"));
        MutableClock clock = new MutableClock(Instant.parse("2026-04-09T10:00:00Z"));
        GroqApiKeyManager manager = new GroqApiKeyManager(providerRepo, apiKeyRepo, clock);

        manager.markKeyOnCooldown("k1");

        assertEquals("k2", manager.acquireAvailableKey());
    }

    @Test
    void acquireAvailableKeyThrowsWhenAllKeysAreCoolingDown() {
        mockKeys(List.of("k1", "k2"));
        MutableClock clock = new MutableClock(Instant.parse("2026-04-09T10:00:00Z"));
        GroqApiKeyManager manager = new GroqApiKeyManager(providerRepo, apiKeyRepo, clock);

        manager.markKeyOnCooldown("k1");
        manager.markKeyOnCooldown("k2");

        assertThrows(IllegalStateException.class, manager::acquireAvailableKey);
    }

    @Test
    void isQuotaOrRateLimitErrorDetectsQuotaMessages() {
        GroqApiKeyManager manager = new GroqApiKeyManager(providerRepo, apiKeyRepo, Clock.systemUTC());

        assertTrue(manager.isQuotaOrRateLimitError(new RuntimeException("RESOURCE_EXHAUSTED: Quota exceeded")));
        assertTrue(manager.isQuotaOrRateLimitError(new RuntimeException("HTTP 429 Too Many Requests")));
        assertFalse(manager.isQuotaOrRateLimitError(new RuntimeException("Invalid API key")));
    }

    private static final class MutableClock extends Clock {
        private final Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

