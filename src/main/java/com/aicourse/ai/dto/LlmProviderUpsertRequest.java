package com.aicourse.ai.dto;

import com.aicourse.ai.AiProviderType;

import java.util.ArrayList;
import java.util.List;

public class LlmProviderUpsertRequest {
    private String code;
    private String displayName;
    private AiProviderType providerType;
    private String modelName;
    private String baseUrl;
    private Integer keyCooldownHours;
    private Boolean enabled;
    private List<String> apiKeys = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AiProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(AiProviderType providerType) {
        this.providerType = providerType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getKeyCooldownHours() {
        return keyCooldownHours;
    }

    public void setKeyCooldownHours(Integer keyCooldownHours) {
        this.keyCooldownHours = keyCooldownHours;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(List<String> apiKeys) {
        this.apiKeys = apiKeys;
    }
}

