package com.leaderboard.service.impl;

import com.auth.repo.UserRepo;
import com.leaderboard.dto.PagedLeaderboardDTO;
import com.leaderboard.model.UserStats;
import com.leaderboard.repository.UserStatsRepository;
import org.springframework.stereotype.Service;

@Service
public class WeeklyLeaderboardService extends AbstractLeaderboardService {

    protected final UserRepo userRepo;

    public WeeklyLeaderboardService(UserStatsRepository userStatsRepository, UserRepo userRepo) {
        super(userStatsRepository);
        this.userRepo = userRepo;
    }

    @Override
    protected int getScore(UserStats user) {
        throw new UnsupportedOperationException("WeeklyLeaderboardService.getScore is not implemented yet");
    }

    public PagedLeaderboardDTO getTopWeeklyUsers(int page, int size) {
        throw new UnsupportedOperationException("WeeklyLeaderboardService.getTopWeeklyUsers is not implemented yet");
    }
}
