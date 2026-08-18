package com.aicourse.ai;

import com.aicourse.geminiConnection.GeminiConnection;
import com.aicourse.groqConnection.GroqConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AiTextClientRouter implements AiTextClient {

    private final GeminiConnection geminiConnection;
    private final Optional<GroqConnection> groqConnection;

    @Value("${app.ai.provider:gemini}")
    private String provider;

    public AiTextClientRouter(GeminiConnection geminiConnection, Optional<GroqConnection> groqConnection) {
        this.geminiConnection = geminiConnection;
        this.groqConnection = groqConnection;
    }

    @Override
    public String getResponse(String prompt) {
        return activeClient().getResponse(prompt);
    }

    @Override
    public Iterable<String> getResponseStream(String prompt) {
        return activeClient().getResponseStream(prompt);
    }

    private AiTextClient activeClient() {
        if ("groq".equalsIgnoreCase(provider)) {
            return groqConnection.orElseThrow(() ->
                    new IllegalStateException("Groq provider selected, but Groq connection is not configured. Set spring.ai.groq.api-keys."));
        }
        return geminiConnection;
    }
}


