package com.aicourse.ai.service;

import com.aicourse.ai.dto.*;
import com.aicourse.ai.model.AiLlmApiKey;
import com.aicourse.ai.model.AiLlmProvider;
import com.aicourse.ai.model.AiLlmRoute;
import com.aicourse.ai.repo.AiLlmApiKeyRepo;
import com.aicourse.ai.repo.AiLlmProviderRepo;
import com.aicourse.ai.repo.AiLlmRouteRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class LlmAdminService {

    private final AiLlmProviderRepo providerRepo;
    private final AiLlmApiKeyRepo apiKeyRepo;
    private final AiLlmRouteRepo routeRepo;
    private final AiDynamicGateway aiDynamicGateway;

    public LlmAdminService(AiLlmProviderRepo providerRepo,
                           AiLlmApiKeyRepo apiKeyRepo,
                           AiLlmRouteRepo routeRepo,
                           AiDynamicGateway aiDynamicGateway) {
        this.providerRepo = providerRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.routeRepo = routeRepo;
        this.aiDynamicGateway = aiDynamicGateway;
    }

    public List<LlmProviderResponse> getProviders() {
        return providerRepo.findAll().stream()
                .map(this::toProviderResponse)
                .collect(Collectors.toList());
    }

    public List<LlmProviderHealthResponse> getProviderHealthSnapshots() {
        return providerRepo.findAll().stream()
                .map(this::toProviderHealthResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LlmProviderResponse upsertProvider(String code, LlmProviderUpsertRequest request) {
        String effectiveCode = sanitizeCode(code != null && !code.isBlank() ? code : request.getCode());
        validateProviderRequest(request, effectiveCode);

        List<String> sanitizedKeys = sanitizeApiKeys(request.getApiKeys());
        if (request.getApiKeys() != null) {
            validateApiKeys(request, sanitizedKeys);
        }

        AiLlmProvider provider = providerRepo.findByCodeIgnoreCase(effectiveCode)
                .orElseGet(AiLlmProvider::new);

        provider.setCode(effectiveCode);
        provider.setDisplayName(request.getDisplayName().trim());
        provider.setProviderType(request.getProviderType());
        provider.setModelName(request.getModelName().trim());
        provider.setBaseUrl(normalizeNullable(request.getBaseUrl()));
        provider.setEnabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()));
        provider.setKeyCooldownHours(request.getKeyCooldownHours() == null ? 24 : Math.max(1, request.getKeyCooldownHours()));

        AiLlmProvider saved = providerRepo.save(provider);

        if (request.getApiKeys() != null) {
            apiKeyRepo.deleteByProvider(saved);
            for (String rawKey : sanitizedKeys) {
                AiLlmApiKey key = new AiLlmApiKey();
                key.setProvider(saved);
                key.setApiKey(rawKey);
                key.setEnabled(true);
                apiKeyRepo.save(key);
            }
        }

        return toProviderResponse(saved);
    }

    public List<LlmRouteResponse> getRoutes() {
        return routeRepo.findAll().stream().map(this::toRouteResponse).collect(Collectors.toList());
    }

    @Transactional
    public LlmRouteResponse upsertRoute(LlmRouteRequest request) {
        if (request.getWorkload() == null) {
            throw new IllegalArgumentException("workload is required");
        }
        if (request.getProviderCode() == null || request.getProviderCode().isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }

        AiLlmProvider provider = providerRepo.findByCodeIgnoreCase(request.getProviderCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + request.getProviderCode()));

        AiLlmRoute route = routeRepo.findByWorkload(request.getWorkload()).orElseGet(AiLlmRoute::new);
        route.setWorkload(request.getWorkload());
        route.setProvider(provider);

        return toRouteResponse(routeRepo.save(route));
    }

    private void validateProviderRequest(LlmProviderUpsertRequest request, String effectiveCode) {
        if (effectiveCode == null || effectiveCode.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (request.getProviderType() == null) {
            throw new IllegalArgumentException("providerType is required");
        }
        if (request.getModelName() == null || request.getModelName().isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
    }

    private List<String> sanitizeApiKeys(List<String> apiKeys) {
        if (apiKeys == null) {
            return List.of();
        }
        return apiKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private void validateApiKeys(LlmProviderUpsertRequest request, List<String> sanitizedKeys) {
        if (sanitizedKeys.isEmpty()) {
            throw new IllegalArgumentException("At least one valid API key is required when apiKeys is provided");
        }

        for (int i = 0; i < sanitizedKeys.size(); i++) {
            String key = sanitizedKeys.get(i);
            try {
                aiDynamicGateway.validateApiKey(
                        request.getProviderType(),
                        request.getModelName(),
                        request.getBaseUrl(),
                        key
                );
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("apiKeys[" + i + "] failed validation: " + ex.getMessage());
            }
        }
    }

    private String sanitizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LlmProviderResponse toProviderResponse(AiLlmProvider provider) {
        List<AiLlmApiKey> keys = apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider);
        List<Long> keyIds = keys.stream().map(AiLlmApiKey::getId).filter(id -> id != null).collect(Collectors.toList());
        AiDynamicGateway.ProviderHealthSnapshot health = aiDynamicGateway.getProviderHealth(provider.getId(), keyIds);

        LlmProviderResponse response = new LlmProviderResponse();
        response.setId(provider.getId());
        response.setCode(provider.getCode());
        response.setDisplayName(provider.getDisplayName());
        response.setProviderType(provider.getProviderType());
        response.setModelName(provider.getModelName());
        response.setBaseUrl(provider.getBaseUrl());
        response.setEnabled(provider.getEnabled());
        response.setKeyCooldownHours(provider.getKeyCooldownHours());
        response.setKeyCount(keys.size());
        response.setActiveKeyCount(health.activeKeyCount());
        response.setCoolingDownKeyCount(health.coolingDownKeyCount());
        response.setLastError(health.lastError());
        response.setLastErrorAt(health.lastErrorAt() == null ? null : health.lastErrorAt().toString());
        response.setLastSuccessAt(health.lastSuccessAt() == null ? null : health.lastSuccessAt().toString());
        response.setMaskedKeys(keys.stream().map(k -> maskKey(k.getApiKey())).collect(Collectors.toCollection(ArrayList::new)));
        return response;
    }

    private LlmProviderHealthResponse toProviderHealthResponse(AiLlmProvider provider) {
        List<Long> keyIds = apiKeyRepo.findByProviderAndEnabledTrueOrderByIdAsc(provider).stream()
                .map(AiLlmApiKey::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        AiDynamicGateway.ProviderHealthSnapshot health = aiDynamicGateway.getProviderHealth(provider.getId(), keyIds);

        LlmProviderHealthResponse response = new LlmProviderHealthResponse();
        response.setProviderCode(provider.getCode());
        response.setActiveKeyCount(health.activeKeyCount());
        response.setCoolingDownKeyCount(health.coolingDownKeyCount());
        response.setLastError(health.lastError());
        response.setLastErrorAt(health.lastErrorAt() == null ? null : health.lastErrorAt().toString());
        response.setLastSuccessAt(health.lastSuccessAt() == null ? null : health.lastSuccessAt().toString());
        return response;
    }

    private LlmRouteResponse toRouteResponse(AiLlmRoute route) {
        LlmRouteResponse response = new LlmRouteResponse();
        response.setWorkload(route.getWorkload());
        response.setProviderCode(route.getProvider().getCode());
        response.setProviderDisplayName(route.getProvider().getDisplayName());
        return response;
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String trimmed = key.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}




