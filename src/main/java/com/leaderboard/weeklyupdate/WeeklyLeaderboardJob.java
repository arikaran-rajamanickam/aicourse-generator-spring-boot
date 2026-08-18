package com.leaderboard.weeklyupdate;

import org.springframework.stereotype.Component;

@Component
public class WeeklyLeaderboardJob {

    protected final WeeklyLeaderboardResetService weeklyLeaderboardResetService;

    public WeeklyLeaderboardJob(WeeklyLeaderboardResetService weeklyLeaderboardResetService) {
        this.weeklyLeaderboardResetService = weeklyLeaderboardResetService;
    }

    public void runWeeklyReset() {
        throw new UnsupportedOperationException("WeeklyLeaderboardJob.runWeeklyReset is not implemented yet");
    }
}
