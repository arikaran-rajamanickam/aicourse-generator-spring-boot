package com.search.dto;

import java.util.List;

public record SearchResponse(List<SearchResultItem> results, long total) {
}

