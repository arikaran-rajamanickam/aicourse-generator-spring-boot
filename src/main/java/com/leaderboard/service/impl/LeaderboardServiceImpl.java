package com.leaderboard.service.impl;

import com.leaderboard.repository.UserStatsRepository;
import com.leaderboard.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;

public class LeaderboardServiceImpl {

    @Autowired
    private static UserStatsRepository repo;

    public static LeaderboardService getInstance() {
        return new GlobalLeaderboardService(repo);
    }
}
