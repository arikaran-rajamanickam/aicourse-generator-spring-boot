package com.aicourse.ai.service;

import com.aicourse.ai.AiProviderType;
import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.model.AiLlmRoute;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmRouteRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiDynamicGateway {

    private static final String DEFAULT_GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final AiLlmRouteRepo routeRepo;
    private final AiLlmApiKeyRepo apiKeyRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final Map<Long, Instant> keyCooldownUntil = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> providerIndex = new ConcurrentHashMap<>();
    private final Map<Long, ProviderRuntimeState> providerRuntime = new ConcurrentHashMap<>();

    public AiDynamicGateway(AiLlmRouteRepo routeRepo,
                            AiLlmApiKeyRepo apiKeyRepo,
                            ObjectMapper objectMapper) {
        this.routeRepo = routeRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String getResponse(AiWorkload workload, String prompt) {
        AiExecutionContext context = resolveContext(workload);
        RuntimeException lastQuotaError = null;

        for (int attempt = 0; attempt < context.keys().size(); attempt++) {
            AiLlmApiKey selected = selectAvailableKey(context.provider(), context.keys());
            try {
                String output = generateOne(context.provider(), selected.getApiKey(), prompt, workload);
                recordSuccess(context.provider().getId());
                return output;
            } catch (RuntimeException ex) {
                recordError(context.provider().getId(), ex);
                if (!isQuotaOrRateLimitError(ex)) {
                    throw ex;
                }
                markCooldown(selected.getId(), context.provider().getKeyCooldownHours());
                lastQuotaError = ex;
            }
        }

        throw new IllegalStateException("All keys for provider '" + context.provider().getCode() + "' are cooling down", lastQuotaError);
    }

    public Iterable<String> getResponseStream(AiWorkload workload, String prompt) {
        AiExecutionContext context = resolveContext(workload);
        RuntimeException lastQuotaError = null;

        for (int attempt = 0; attempt < context.keys().size(); attempt++) {
            AiLlmApiKey selected = selectAvailableKey(context.provider(), context.keys());
            try {
                Iterable<String> stream = generateStream(context.provider(), selected.getApiKey(), prompt, workload);
                recordSuccess(context.provider().getId());
                return stream;
            } catch (RuntimeException ex) {
                recordError(context.provider().getId(), ex);
                if (!isQuotaOrRateLimitError(ex)) {
                    throw ex;
                }
                markCooldown(selected.getId(), context.provider().getKeyCooldownHours());
                lastQuotaError = ex;
            }
        }

        throw new IllegalStateException("All keys for provider '" + context.provider().getCode() + "' are cooling down", lastQuotaError);
    }

    private AiExecutionContext resolveContext(AiWorkload workload) {
        AiLlmRoute route = routeRepo.findByWorkload(workload)
                .orElseThrow(() -> new IllegalStateException("No LLM route configured for workload " + workload.name()));

        AiLlmProvider provider = route.getProvider();
        if (!Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("Provider '" + provider.getCode() + "' is disabled for workload " + workload.name());
        }

        List<AiLlmApiKey> keys = apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider);
        if (keys.isEmpty()) {
            throw new IllegalStateException("No active API keys configured for provider " + provider.getCode());
        }
        return new AiExecutionContext(provider, keys);
    }

    private AiLlmApiKey selectAvailableKey(AiLlmProvider provider, List<AiLlmApiKey> keys) {
        int size = keys.size();
        AtomicInteger index = providerIndex.computeIfAbsent(provider.getId(), ignored -> new AtomicInteger(0));
        Instant now = Instant.now();

        for (int attempt = 0; attempt < size; attempt++) {
            int i = Math.floorMod(index.getAndIncrement(), size);
            AiLlmApiKey key = keys.get(i);
            Instant coolUntil = keyCooldownUntil.get(key.getId());
            if (coolUntil == null || !coolUntil.isAfter(now)) {
                keyCooldownUntil.remove(key.getId());
                return key;
            }
        }

        Instant earliest = keys.stream()
                .map(k -> keyCooldownUntil.getOrDefault(k.getId(), now))
                .min(Comparator.naturalOrder())
                .orElse(now);
        long waitSeconds = Math.max(1, Duration.between(now, earliest).getSeconds());
        throw new IllegalStateException("All keys for provider '" + provider.getCode() + "' are cooling down. Retry in " + waitSeconds + "s");
    }

    private void markCooldown(Long keyId, Integer cooldownHours) {
        int hours = cooldownHours == null || cooldownHours < 1 ? 24 : cooldownHours;
        keyCooldownUntil.put(keyId, Instant.now().plus(Duration.ofHours(hours)));
    }

    private String generateOne(AiLlmProvider provider, String apiKey, String prompt, AiWorkload workload) {
        if (provider.getProviderType() == AiProviderType.GROQ) {
            return generateGroqText(provider, apiKey, prompt, workload);
        }
        return generateGeminiText(provider, apiKey, prompt, workload);
    }

    private Iterable<String> generateStream(AiLlmProvider provider, String apiKey, String prompt, AiWorkload workload) {
        if (provider.getProviderType() == AiProviderType.GROQ) {
            return generateGroqStream(provider, apiKey, prompt, workload);
        }

        Client client = Client.builder().apiKey(apiKey).build();
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
        if (requiresJsonOutput(workload)) {
            configBuilder.responseMimeType("application/json");
        }
        GenerateContentConfig config = configBuilder.build();
        Iterable<GenerateContentResponse> stream = client.models.generateContentStream(provider.getModelName(), prompt, config);
        return textChunks(stream);
    }

    private String generateGeminiText(AiLlmProvider provider, String apiKey, String prompt, AiWorkload workload) {
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();
            if (requiresJsonOutput(workload)) {
                configBuilder.responseMimeType("application/json");
            }
            GenerateContentConfig config = configBuilder.build();

            GenerateContentResponse response = client.models.generateContent(provider.getModelName(), prompt, config);
            return response.text();
        }
    }

    private String generateGroqText(AiLlmProvider provider, String apiKey, String prompt, AiWorkload workload) {
        try {
            String target = provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()
                    ? DEFAULT_GROQ_BASE_URL
                    : provider.getBaseUrl();

            boolean jsonOutput = requiresJsonOutput(workload);
            List<Map<String, String>> messages = new ArrayList<>();
            if (jsonOutput) {
                messages.add(Map.of(
                        "role", "system",
                        "content", "Return only valid JSON. Do not use markdown fences or extra text."
                ));
            }
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", provider.getModelName());
            payload.put("messages", messages);
            payload.put("temperature", jsonOutput ? 0.0 : 0.2);
            if (jsonOutput) {
                payload.put("response_format", Map.of("type", "json_object"));
            }

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Groq API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                throw new RuntimeException("Groq response missing choices[0].message.content");
            }
            return contentNode.asText();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private Iterable<String> textChunks(Iterable<GenerateContentResponse> stream) {
        return () -> new Iterator<>() {
            private final Iterator<GenerateContentResponse> delegate = stream.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String next() {
                GenerateContentResponse chunk = delegate.next();
                return Objects.toString(chunk.text(), "");
            }
        };
    }

    private Iterable<String> generateGroqStream(AiLlmProvider provider, String apiKey, String prompt, AiWorkload workload) {
        String target = provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()
                ? DEFAULT_GROQ_BASE_URL
                : provider.getBaseUrl();

        try {
            boolean jsonOutput = requiresJsonOutput(workload);
            List<Map<String, String>> messages = new ArrayList<>();
            if (jsonOutput) {
                messages.add(Map.of(
                        "role", "system",
                        "content", "Return only valid JSON. Do not use markdown fences or extra text."
                ));
            }
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", provider.getModelName());
            payload.put("messages", messages);
            payload.put("temperature", jsonOutput ? 0.0 : 0.2);
            payload.put("stream", true);

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("Groq API error " + response.statusCode() + ": " + errorBody);
            }

            return groqSseChunks(response.body());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private Iterable<String> groqSseChunks(InputStream stream) {
        return () -> new Iterator<>() {
            private final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            private boolean finished = false;
            private String nextChunk = readNextChunk();

            @Override
            public boolean hasNext() {
                if (nextChunk != null) {
                    return true;
                }
                closeIfFinished();
                return false;
            }

            @Override
            public String next() {
                if (nextChunk == null) {
                    throw new java.util.NoSuchElementException("No more SSE chunks");
                }
                String current = nextChunk;
                nextChunk = readNextChunk();
                if (nextChunk == null) {
                    closeIfFinished();
                }
                return current;
            }

            private String readNextChunk() {
                if (finished) {
                    return null;
                }
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        String payload = line.substring(5).trim();
                        if (payload.isBlank()) {
                            continue;
                        }
                        if ("[DONE]".equals(payload)) {
                            finished = true;
                            return null;
                        }

                        String token = parseGroqSseContent(payload);
                        if (token != null && !token.isEmpty()) {
                            return token;
                        }
                    }
                    finished = true;
                    return null;
                } catch (Exception ex) {
                    finished = true;
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }

            private void closeIfFinished() {
                if (finished) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        };
    }

    String parseGroqSseContent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return null;
            }
            return contentNode.asText();
        } catch (Exception ex) {
            throw new RuntimeException("Invalid Groq SSE chunk: " + ex.getMessage(), ex);
        }
    }

    public ProviderHealthSnapshot getProviderHealth(Long providerId, List<Long> keyIds) {
        Instant now = Instant.now();
        int coolingDown = 0;

        for (Long keyId : keyIds) {
            Instant cooldownUntil = keyCooldownUntil.get(keyId);
            if (cooldownUntil == null) {
                continue;
            }
            if (cooldownUntil.isAfter(now)) {
                coolingDown++;
            } else {
                keyCooldownUntil.remove(keyId);
            }
        }

        ProviderRuntimeState runtime = providerRuntime.computeIfAbsent(providerId, ignored -> new ProviderRuntimeState());
        int active = Math.max(0, keyIds.size() - coolingDown);
        return new ProviderHealthSnapshot(active, coolingDown, runtime.lastError, runtime.lastErrorAt, runtime.lastSuccessAt);
    }

    public void validateApiKey(AiProviderType providerType,
                               String modelName,
                               String baseUrl,
                               String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be empty");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name is required for key validation");
        }

        AiLlmProvider probeProvider = new AiLlmProvider();
        probeProvider.setProviderType(providerType);
        probeProvider.setModelName(modelName.trim());
        probeProvider.setBaseUrl(baseUrl);

        try {
            if (providerType == AiProviderType.GROQ) {
                String test = generateGroqText(probeProvider, apiKey.trim(), "Reply with exactly: ok", AiWorkload.AI_COACH);
                if (test == null || test.isBlank()) {
                    throw new IllegalArgumentException("Groq test response was empty");
                }
                return;
            }

            String test = generateGeminiText(probeProvider, apiKey.trim(), "Reply with exactly: ok", AiWorkload.AI_COACH);
            if (test == null || test.isBlank()) {
                throw new IllegalArgumentException("Gemini test response was empty");
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("API key validation failed: " + ex.getMessage(), ex);
        }
    }

    private void recordSuccess(Long providerId) {
        ProviderRuntimeState state = providerRuntime.computeIfAbsent(providerId, ignored -> new ProviderRuntimeState());
        state.lastSuccessAt = Instant.now();
        state.lastError = null;
        state.lastErrorAt = null;
    }

    private void recordError(Long providerId, Exception ex) {
        ProviderRuntimeState state = providerRuntime.computeIfAbsent(providerId, ignored -> new ProviderRuntimeState());
        state.lastError = ex.getMessage();
        state.lastErrorAt = Instant.now();
    }

    private boolean requiresJsonOutput(AiWorkload workload) {
        return workload == AiWorkload.COURSE_GENERATION || workload == AiWorkload.LESSON_GENERATION;
    }

    private boolean isQuotaOrRateLimitError(Throwable error) {
        String combined = combineMessages(error).toLowerCase(Locale.ROOT);
        return combined.contains("resource_exhausted")
                || combined.contains("quota")
                || combined.contains("rate limit")
                || combined.contains("too many requests")
                || combined.contains("429");
    }

    private String combineMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    private record AiExecutionContext(AiLlmProvider provider, List<AiLlmApiKey> keys) {
    }

    public record ProviderHealthSnapshot(int activeKeyCount,
                                         int coolingDownKeyCount,
                                         String lastError,
                                         Instant lastErrorAt,
                                         Instant lastSuccessAt) {
    }

    private static final class ProviderRuntimeState {
        private volatile String lastError;
        private volatile Instant lastErrorAt;
        private volatile Instant lastSuccessAt;
    }
}



