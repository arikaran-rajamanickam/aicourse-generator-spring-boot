package com.marketing.service;

import com.marketing.dto.MarketingContentResponse;

public interface MarketingContentService {
    MarketingContentResponse getPublishedPage(String pageKey);
}

