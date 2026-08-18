package com.aicourse.ai.dto;

import com.aicourse.ai.AiProviderType;

import java.util.List;

public class LlmProviderResponse {
    private Long id;
    private String code;
    private String displayName;
    private AiProviderType providerType;
    private String modelName;
    private String baseUrl;
    private Integer keyCooldownHours;
    private Boolean enabled;
    private Integer keyCount;
    private Integer activeKeyCount;
    private Integer coolingDownKeyCount;
    private String lastError;
    private String lastErrorAt;
    private String lastSuccessAt;
    private List<String> maskedKeys;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getKeyCount() {
        return keyCount;
    }

    public void setKeyCount(Integer keyCount) {
        this.keyCount = keyCount;
    }

    public Integer getActiveKeyCount() {
        return activeKeyCount;
    }

    public void setActiveKeyCount(Integer activeKeyCount) {
        this.activeKeyCount = activeKeyCount;
    }

    public Integer getCoolingDownKeyCount() {
        return coolingDownKeyCount;
    }

    public void setCoolingDownKeyCount(Integer coolingDownKeyCount) {
        this.coolingDownKeyCount = coolingDownKeyCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getLastErrorAt() {
        return lastErrorAt;
    }

    public void setLastErrorAt(String lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }

    public String getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(String lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public List<String> getMaskedKeys() {
        return maskedKeys;
    }

    public void setMaskedKeys(List<String> maskedKeys) {
        this.maskedKeys = maskedKeys;
    }
}


