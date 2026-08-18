package com.search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

public record SearchResultItem(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Long id,
        ResultType type,
        String label,
        String description,
        double score,
        String handle) {
}
