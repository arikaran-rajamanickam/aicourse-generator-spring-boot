package com.aicourse.ai.service;

import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmRouteRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AiDynamicGatewayTest {

    @Test
    void parseGroqSseContentReturnsDeltaToken() {
        AiDynamicGateway gateway = new AiDynamicGateway(mock(AiLlmRouteRepo.class), mock(AiLlmApiKeyRepo.class), new ObjectMapper());

        String token = gateway.parseGroqSseContent("{\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");

        assertEquals("hello", token);
    }

    @Test
    void parseGroqSseContentReturnsNullWhenNoContent() {
        AiDynamicGateway gateway = new AiDynamicGateway(mock(AiLlmRouteRepo.class), mock(AiLlmApiKeyRepo.class), new ObjectMapper());

        String token = gateway.parseGroqSseContent("{\"choices\":[{\"delta\":{}}]}");

        assertNull(token);
    }

    @Test
    void parseGroqSseContentThrowsOnMalformedJson() {
        AiDynamicGateway gateway = new AiDynamicGateway(mock(AiLlmRouteRepo.class), mock(AiLlmApiKeyRepo.class), new ObjectMapper());

        assertThrows(RuntimeException.class, () -> gateway.parseGroqSseContent("not-json"));
    }
}

