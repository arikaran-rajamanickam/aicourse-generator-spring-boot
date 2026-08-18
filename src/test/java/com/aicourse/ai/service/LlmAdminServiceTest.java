package com.aicourse.ai.service;

import com.aicourse.ai.AiProviderType;
import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.dto.LlmProviderUpsertRequest;
import com.aicourse.ai.dto.LlmRouteRequest;
import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.model.AiLlmRoute;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmProviderRepo;
import com.aicourse.ai.repo.AiLlmRouteRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LlmAdminServiceTest {

    @Test
    void upsertProviderRequiresBasicFields() {
        LlmAdminService service = new LlmAdminService(
                mock(AiLlmProviderRepo.class),
                mock(AiLlmApiKeyRepo.class),
                mock(AiLlmRouteRepo.class),
                mock(AiDynamicGateway.class)
        );

        LlmProviderUpsertRequest request = new LlmProviderUpsertRequest();
        request.setCode("test");

        assertThrows(IllegalArgumentException.class, () -> service.upsertProvider(null, request));
    }

    @Test
    void upsertRouteBindsProviderToWorkload() {
        AiLlmProviderRepo providerRepo = mock(AiLlmProviderRepo.class);
        AiLlmApiKeyRepo keyRepo = mock(AiLlmApiKeyRepo.class);
        AiLlmRouteRepo routeRepo = mock(AiLlmRouteRepo.class);

        AiLlmProvider provider = new AiLlmProvider();
        provider.setCode("gemini");
        provider.setDisplayName("Gemini");
        when(providerRepo.findByCodeIgnoreCase("gemini")).thenReturn(Optional.of(provider));
        when(routeRepo.findByWorkload(AiWorkload.AI_COACH)).thenReturn(Optional.empty());
        when(routeRepo.save(ArgumentMatchers.any(AiLlmRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LlmAdminService service = new LlmAdminService(providerRepo, keyRepo, routeRepo, mock(AiDynamicGateway.class));

        LlmRouteRequest request = new LlmRouteRequest();
        request.setWorkload(AiWorkload.AI_COACH);
        request.setProviderCode("gemini");

        var response = service.upsertRoute(request);

        assertEquals(AiWorkload.AI_COACH, response.getWorkload());
        assertEquals("gemini", response.getProviderCode());
        verify(routeRepo, times(1)).save(any(AiLlmRoute.class));
    }

    @Test
    void getProvidersReturnsMaskedKeys() {
        AiLlmProviderRepo providerRepo = mock(AiLlmProviderRepo.class);
        AiLlmApiKeyRepo keyRepo = mock(AiLlmApiKeyRepo.class);
        AiLlmRouteRepo routeRepo = mock(AiLlmRouteRepo.class);
        AiDynamicGateway gateway = mock(AiDynamicGateway.class);

        AiLlmProvider provider = new AiLlmProvider();
        provider.setId(1L);
        provider.setCode("groq");
        provider.setDisplayName("Groq");
        provider.setProviderType(AiProviderType.GROQ);
        provider.setModelName("llama");
        provider.setEnabled(true);
        provider.setKeyCooldownHours(24);

        var key = new com.aicourse.ai.model.AiLlmApiKey();
        key.setId(10L);
        key.setApiKey("gsk_abcdefghijklmnopqrstuvwxyz");

        when(providerRepo.findAll()).thenReturn(List.of(provider));
        when(keyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider)).thenReturn(List.of(key));

        when(gateway.getProviderHealth(eq(1L), eq(List.of(10L))))
                .thenReturn(new AiDynamicGateway.ProviderHealthSnapshot(1, 0, null, null, null));

        LlmAdminService service = new LlmAdminService(providerRepo, keyRepo, routeRepo, gateway);
        var providers = service.getProviders();

        assertEquals(1, providers.size());
        assertEquals(1, providers.get(0).getMaskedKeys().size());
        assertEquals(1, providers.get(0).getActiveKeyCount());
    }

    @Test
    void getProviderHealthSnapshotsReturnsProviderCodeAndCounts() {
        AiLlmProviderRepo providerRepo = mock(AiLlmProviderRepo.class);
        AiLlmApiKeyRepo keyRepo = mock(AiLlmApiKeyRepo.class);
        AiLlmRouteRepo routeRepo = mock(AiLlmRouteRepo.class);
        AiDynamicGateway gateway = mock(AiDynamicGateway.class);

        AiLlmProvider provider = new AiLlmProvider();
        provider.setId(99L);
        provider.setCode("gemini");

        var key = new com.aicourse.ai.model.AiLlmApiKey();
        key.setId(101L);
        when(providerRepo.findAll()).thenReturn(List.of(provider));
        when(keyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider)).thenReturn(List.of(key));
        when(gateway.getProviderHealth(eq(99L), eq(List.of(101L))))
                .thenReturn(new AiDynamicGateway.ProviderHealthSnapshot(1, 0, null, null, null));

        LlmAdminService service = new LlmAdminService(providerRepo, keyRepo, routeRepo, gateway);
        var snapshots = service.getProviderHealthSnapshots();

        assertFalse(snapshots.isEmpty());
        assertEquals("gemini", snapshots.get(0).getProviderCode());
        assertEquals(1, snapshots.get(0).getActiveKeyCount());
    }

    @Test
    void upsertProviderValidatesEverySubmittedApiKeyBeforeSaving() {
        AiLlmProviderRepo providerRepo = mock(AiLlmProviderRepo.class);
        AiLlmApiKeyRepo keyRepo = mock(AiLlmApiKeyRepo.class);
        AiLlmRouteRepo routeRepo = mock(AiLlmRouteRepo.class);
        AiDynamicGateway gateway = mock(AiDynamicGateway.class);

        AiLlmProvider persisted = new AiLlmProvider();
        persisted.setId(7L);
        when(providerRepo.save(any(AiLlmProvider.class))).thenReturn(persisted);
        when(keyRepo.findByProviderAndEnabledTrueOrderByIdAsc(persisted)).thenReturn(List.of());
        when(gateway.getProviderHealth(eq(7L), eq(List.of()))).thenReturn(new AiDynamicGateway.ProviderHealthSnapshot(0, 0, null, null, null));

        LlmProviderUpsertRequest request = new LlmProviderUpsertRequest();
        request.setCode("groq-main");
        request.setDisplayName("Groq Main");
        request.setProviderType(AiProviderType.GROQ);
        request.setModelName("llama-3.1-8b-instant");
        request.setApiKeys(List.of("gsk_test_1", "gsk_test_2"));

        LlmAdminService service = new LlmAdminService(providerRepo, keyRepo, routeRepo, gateway);
        service.upsertProvider(null, request);

        verify(gateway, times(2)).validateApiKey(eq(AiProviderType.GROQ), eq("llama-3.1-8b-instant"), isNull(), anyString());
        verify(keyRepo, times(2)).save(any(AiLlmApiKey.class));
    }

    @Test
    void upsertProviderRejectsWhenAnyApiKeyValidationFails() {
        AiLlmProviderRepo providerRepo = mock(AiLlmProviderRepo.class);
        AiLlmApiKeyRepo keyRepo = mock(AiLlmApiKeyRepo.class);
        AiLlmRouteRepo routeRepo = mock(AiLlmRouteRepo.class);
        AiDynamicGateway gateway = mock(AiDynamicGateway.class);

        doThrow(new IllegalArgumentException("API key validation failed: 401 Unauthorized"))
                .when(gateway)
                .validateApiKey(eq(AiProviderType.GEMINI), eq("gemini-2.5-flash"), isNull(), anyString());

        LlmProviderUpsertRequest request = new LlmProviderUpsertRequest();
        request.setCode("gemini-main");
        request.setDisplayName("Gemini Main");
        request.setProviderType(AiProviderType.GEMINI);
        request.setModelName("gemini-2.5-flash");
        request.setApiKeys(List.of("bad_key"));

        LlmAdminService service = new LlmAdminService(providerRepo, keyRepo, routeRepo, gateway);

        assertThrows(IllegalArgumentException.class, () -> service.upsertProvider(null, request));
        verify(providerRepo, never()).save(any(AiLlmProvider.class));
        verify(keyRepo, never()).save(any(AiLlmApiKey.class));
    }
}



