package com.search.dto;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SearchRequest {
    private String query;
    private List<ResultType> types;
    private int offset = 0;
    private int limit = 10;
    private Set<Long> excludeUserIds = Collections.emptySet();

    public SearchRequest() {
    }

    public SearchRequest(String query, List<ResultType> types, int offset, int limit) {
        this.query = query;
        this.types = types;
        this.offset = offset;
        this.limit = limit;
    }

    public SearchRequest(String query, List<ResultType> types, int offset, int limit, Set<Long> excludeUserIds) {
        this.query = query;
        this.types = types;
        this.offset = offset;
        this.limit = limit;
        this.excludeUserIds = excludeUserIds == null ? Collections.emptySet() : excludeUserIds;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<ResultType> getTypes() {
        return types == null ? Collections.emptyList() : types;
    }

    public void setTypes(List<ResultType> types) {
        this.types = types;
    }

    public int getOffset() {
        return Math.max(0, offset);
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return Math.max(1, Math.min(limit, 50));
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Set<Long> getExcludeUserIds() {
        return excludeUserIds == null ? Collections.emptySet() : excludeUserIds;
    }

    public void setExcludeUserIds(Set<Long> excludeUserIds) {
        this.excludeUserIds = excludeUserIds == null ? Collections.emptySet() : excludeUserIds;
    }
}

