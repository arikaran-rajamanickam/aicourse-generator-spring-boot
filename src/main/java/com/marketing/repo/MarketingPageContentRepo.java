package com.marketing.repo;

import com.marketing.model.MarketingPageContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketingPageContentRepo extends JpaRepository<MarketingPageContent, Long> {
    Optional<MarketingPageContent> findByPageKeyIgnoreCase(String pageKey);
}

