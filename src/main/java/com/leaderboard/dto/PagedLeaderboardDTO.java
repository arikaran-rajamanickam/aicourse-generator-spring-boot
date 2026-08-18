package com.leaderboard.dto;

import java.util.List;

public class PagedLeaderboardDTO {
    private final List<LeaderboardResponseDTO> data;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PagedLeaderboardDTO(List<LeaderboardResponseDTO> data, int page, int size, long totalElements, int totalPages) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<LeaderboardResponseDTO> getData() {
        return data;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}