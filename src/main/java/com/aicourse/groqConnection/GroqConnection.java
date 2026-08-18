package com.aicourse.groqConnection;

import com.aicourse.ai.AiTextClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
public class GroqConnection implements AiTextClient {

    private static final Logger LOGGER = Logger.getLogger(GroqConnection.class.getName());

    private final GroqApiKeyManager apiKeyManager;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${spring.ai.groq.base-url:https://api.groq.com/openai/v1/chat/completions}")
    private String baseUrl;

    @Value("${spring.ai.groq.chat.options.model:llama3-8b-8192}")
    private String model;

    public GroqConnection(GroqApiKeyManager apiKeyManager,
                          ObjectMapper objectMapper) {
        this.apiKeyManager = apiKeyManager;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String getResponse(String prompt) {
        return executeWithFailover(apiKey -> invokeGroqOnce(apiKey, prompt));
    }

    @Override
    public Iterable<String> getResponseStream(String prompt) {
        // Groq SSE streaming can be added later; fallback keeps the same contract for UI integration.
        return List.of(getResponse(prompt));
    }

    private String invokeGroqOnce(String apiKey, String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.2
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
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
                throw new RuntimeException("Groq API response did not contain choices[0].message.content");
            }
            return contentNode.asText();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String executeWithFailover(Function<String, String> operation) {
        RuntimeException lastQuotaOrRateLimitError = null;

        for (int attempt = 0; attempt < apiKeyManager.totalKeyCount(); attempt++) {
            String key = apiKeyManager.acquireAvailableKey();
            try {
                return operation.apply(key);
            } catch (RuntimeException error) {
                if (!apiKeyManager.isQuotaOrRateLimitError(error)) {
                    LOGGER.log(Level.SEVERE, "Groq API call failed with non-retryable error: {0}", error.getMessage());
                    throw error;
                }

                apiKeyManager.markKeyOnCooldown(key);
                lastQuotaOrRateLimitError = error;
                LOGGER.log(Level.WARNING, "Groq API key hit quota/rate limit and is cooled down for 24h. Trying next key.");
            }
        }

        if (lastQuotaOrRateLimitError != null) {
            throw new IllegalStateException("All Groq API keys have hit quota/rate limits and are cooling down.", lastQuotaOrRateLimitError);
        }

        throw new IllegalStateException("No Groq API key available.");
    }
}

