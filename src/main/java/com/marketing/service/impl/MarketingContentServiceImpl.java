package com.marketing.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketing.dto.MarketingContentResponse;
import com.marketing.model.MarketingPageContent;
import com.marketing.repo.MarketingPageContentRepo;
import com.marketing.service.MarketingContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketingContentServiceImpl implements MarketingContentService {

    @Autowired
    private MarketingPageContentRepo contentRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MarketingContentResponse getPublishedPage(String pageKey) {
        MarketingPageContent page = contentRepo.findByPageKeyIgnoreCase(pageKey)
                .orElseThrow(() -> new RuntimeException("Marketing page not found: " + pageKey));

        try {
            JsonNode payload = objectMapper.readTree(page.getContent());
            return new MarketingContentResponse(
                    page.getPageKey(),
                    page.getVersion(),
                    page.getUpdatedAt(),
                    payload
            );
        } catch (Exception ex) {
            throw new RuntimeException("Invalid marketing JSON payload for page: " + pageKey, ex);
        }
    }
}

