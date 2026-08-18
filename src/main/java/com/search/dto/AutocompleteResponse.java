package com.search.dto;

import java.util.List;

public record AutocompleteResponse(List<String> suggestions, List<SearchResultItem> topResults) {
}

