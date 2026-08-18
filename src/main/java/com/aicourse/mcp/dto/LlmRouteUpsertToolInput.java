package com.aicourse.mcp.dto;

import com.aicourse.ai.AiWorkload;
import com.aicourse.ai.dto.LlmRouteRequest;

public class LlmRouteUpsertToolInput {

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

    public LlmRouteRequest toRequest() {
        LlmRouteRequest request = new LlmRouteRequest();
        request.setWorkload(workload);
        request.setProviderCode(providerCode);
        return request;
    }
}

