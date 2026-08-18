package com.marketing.controller;

import com.marketing.dto.MarketingContentResponse;
import com.marketing.service.MarketingContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/content/public")
public class MarketingContentController {

    @Autowired
    private MarketingContentService marketingContentService;

    @GetMapping("/{pageKey}")
    public ResponseEntity<?> getPage(@PathVariable String pageKey, org.springframework.web.context.request.WebRequest request) {
        try {
            MarketingContentResponse response = marketingContentService.getPublishedPage(pageKey.toLowerCase(Locale.ROOT));
            String etag = "\"" + response.pageKey() + "-v" + response.version() + "\"";

            if (request.checkNotModified(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .eTag(etag)
                        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                        .build();
            }

            return ResponseEntity.ok()
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                    .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING)
                    .body(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}

