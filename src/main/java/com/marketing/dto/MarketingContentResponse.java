package com.marketing.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record MarketingContentResponse(
        String pageKey,
        Long version,
        OffsetDateTime updatedAt,
        JsonNode content
) {
}

