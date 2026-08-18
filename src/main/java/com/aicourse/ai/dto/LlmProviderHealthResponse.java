package com.aicourse.ai.dto;

public class LlmProviderHealthResponse {
    private String providerCode;
    private Integer activeKeyCount;
    private Integer coolingDownKeyCount;
    private String lastError;
    private String lastErrorAt;
    private String lastSuccessAt;

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
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
}

