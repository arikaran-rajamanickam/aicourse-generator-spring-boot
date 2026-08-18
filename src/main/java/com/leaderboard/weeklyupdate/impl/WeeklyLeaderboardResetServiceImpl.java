package com.leaderboard.weeklyupdate.impl;

import com.leaderboard.repository.UserStatsRepository;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardResetService;
import org.springframework.stereotype.Service;

@Service
public class WeeklyLeaderboardResetServiceImpl implements WeeklyLeaderboardResetService {

    protected final UserStatsRepository userStatsRepository;

    public WeeklyLeaderboardResetServiceImpl(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    @Override
    public int resetWeeklyLeaderboard() {
        throw new UnsupportedOperationException("WeeklyLeaderboardResetServiceImpl.resetWeeklyLeaderboard is not implemented yet");
    }
}
