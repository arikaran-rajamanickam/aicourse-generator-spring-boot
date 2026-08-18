package com.aicourse.ai.dto;

import com.aicourse.ai.AiWorkload;

public class LlmRouteRequest {
    private AiWorkload workload;
    private String providerCode;

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
}

