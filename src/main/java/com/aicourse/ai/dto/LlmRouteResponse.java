package com.aicourse.ai.dto;

import com.aicourse.ai.AiWorkload;

public class LlmRouteResponse {
    private AiWorkload workload;
    private String providerCode;
    private String providerDisplayName;

    public AiWorkload getWorkload() {
        return workload;
    }

    public void setWorkload(AiWorkload workload) {
        this.workload = workload;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderDisplayName() {
        return providerDisplayName;
    }

    public void setProviderDisplayName(String providerDisplayName) {
        this.providerDisplayName = providerDisplayName;
    }
}

