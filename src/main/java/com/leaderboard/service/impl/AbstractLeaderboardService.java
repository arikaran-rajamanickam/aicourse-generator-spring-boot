package com.leaderboard.service.impl;

import com.leaderboard.dto.LeaderboardResponseDTO;
import com.leaderboard.model.UserStats;
import com.leaderboard.repository.UserStatsRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLeaderboardService {

    protected final UserStatsRepository userStatsRepository;

    protected AbstractLeaderboardService(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    protected List<LeaderboardResponseDTO> buildLeaderBoard(List<UserStats> stats, int offset) {

        AtomicInteger rank = new AtomicInteger(offset + 1);

        return stats.stream()
                .map(user -> new LeaderboardResponseDTO(
                        rank.getAndIncrement(),
                        user.getUserId(),
                        getScore(user),
                        null, // displayName will be populated later
                        null, // handle will be populated later
                        user.getTotalCoursesCreated(),
                        user.getCurrentStreak(),
                        user.getWeeklyPoints()
                ))
                .toList();
    }

    protected abstract int getScore(UserStats user);
}