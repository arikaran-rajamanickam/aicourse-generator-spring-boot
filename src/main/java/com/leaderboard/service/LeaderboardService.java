package com.leaderboard.service;

import com.leaderboard.dto.PagedLeaderboardDTO;
import com.leaderboard.dto.UserRankDTO;

public interface LeaderboardService {
    PagedLeaderboardDTO getTopGlobalUsers(int page, int size) throws Exception;

    UserRankDTO getUserRankDTO(Long userId) throws Exception;
}