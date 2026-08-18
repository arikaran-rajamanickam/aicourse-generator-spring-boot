package com.search.model;

import com.search.dto.ResultType;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

public class SearchDocument {
    private final String key;
    private final Long id;
    private final ResultType type;
    private final String title;
    private final String description;
    private final String handle;
    private final OffsetDateTime createdAt;
    private final double popularityWeight;
    private final Set<String> tokens;

    public SearchDocument(Long id, ResultType type, String title, String description,
                          String handle, OffsetDateTime createdAt, double popularityWeight, Set<String> tokens) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.handle = handle;
        this.createdAt = createdAt;
        this.popularityWeight = popularityWeight;
        this.tokens = tokens == null ? Collections.emptySet() : tokens;
        this.key = type.name() + ":" + id;
    }

    public String getKey() {
        return key;
    }

    public Long getId() {
        return id;
    }

    public ResultType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getHandle() {
        return handle;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public double getPopularityWeight() {
        return popularityWeight;
    }

    public Set<String> getTokens() {
        return tokens;
    }
}
