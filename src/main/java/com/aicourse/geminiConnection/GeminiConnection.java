package com.aicourse.geminiConnection;

import com.aicourse.ai.AiTextClient;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class GeminiConnection implements AiTextClient {

    private static final Logger LOGGER = Logger.getLogger(GeminiConnection.class.getName());

    private final GeminiApiKeyManager apiKeyManager;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String model;

    public GeminiConnection(GeminiApiKeyManager apiKeyManager) {
        this.apiKeyManager = apiKeyManager;
    }

    @Override
    public String getResponse(String prompt) {
        LOGGER.log(Level.FINE, "Prompt sent to Gemini ({0}) chars", new Object[]{prompt.length()});
        return executeWithFailover(client -> {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .build();

            GenerateContentResponse response = client.models.generateContent(model, prompt, config);
            LOGGER.log(Level.FINE, "Gemini response received. Length: {0}", new Object[]{response.text().length()});
            return response.text();
        });
    }

    @Override
    public Iterable<String> getResponseStream(String prompt) {
        LOGGER.log(Level.FINE, "Streaming Prompt sent to Gemini ({0}) chars", new Object[]{prompt.length()});
        return executeWithFailover(client -> {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .build();

            Iterable<GenerateContentResponse> stream = client.models.generateContentStream(model, prompt, config);
            return textChunks(stream);
        });
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

    private <T> T executeWithFailover(Function<Client, T> operation) {
        RuntimeException lastQuotaOrRateLimitError = null;

        for (int attempt = 0; attempt < apiKeyManager.totalKeyCount(); attempt++) {
            String key = apiKeyManager.acquireAvailableKey();
            Client client = Client.builder().apiKey(key).build();

            try {
                return operation.apply(client);
            } catch (RuntimeException error) {
                if (!apiKeyManager.isQuotaOrRateLimitError(error)) {
                    LOGGER.log(Level.SEVERE, "Gemini API call failed with non-retryable error: {0}", error.getMessage());
                    throw error;
                }

                apiKeyManager.markKeyOnCooldown(key);
                lastQuotaOrRateLimitError = error;
                LOGGER.log(Level.WARNING, "Gemini API key hit quota/rate limit and is cooled down for 24h. Trying next key.");
            }
        }

        if (lastQuotaOrRateLimitError != null) {
            throw new IllegalStateException("All Gemini API keys have hit quota/rate limits and are cooling down.", lastQuotaOrRateLimitError);
        }

        throw new IllegalStateException("No Gemini API key available.");
    }

}
