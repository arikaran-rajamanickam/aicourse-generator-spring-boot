package com.search.controller;

import com.search.dto.AutocompleteResponse;
import com.search.dto.ResultType;
import com.search.dto.SearchRequest;
import com.search.dto.SearchResponse;
import com.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam(value = "types", required = false) String types,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "excludeIds", required = false) String excludeIds) {

        List<ResultType> resolvedTypes = parseTypes(types);
        SearchRequest request = new SearchRequest(query, resolvedTypes, offset, limit, parseIds(excludeIds));
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<AutocompleteResponse> autocomplete(
            @RequestParam("q") String prefix,
            @RequestParam(value = "types", required = false) String types,
            @RequestParam(value = "limit", defaultValue = "8") int limit,
            @RequestParam(value = "excludeIds", required = false) String excludeIds) {

        List<ResultType> resolvedTypes = parseTypes(types);
        AutocompleteResponse response = searchService.autocomplete(prefix, resolvedTypes, limit, parseIds(excludeIds));
        return ResponseEntity.ok(response);
    }

    private List<ResultType> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(String::toUpperCase)
                .map(value -> {
                    try {
                        return ResultType.valueOf(value);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(type -> type != null)
                .collect(Collectors.toList());
    }

    private Set<Long> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (String part : raw.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}


