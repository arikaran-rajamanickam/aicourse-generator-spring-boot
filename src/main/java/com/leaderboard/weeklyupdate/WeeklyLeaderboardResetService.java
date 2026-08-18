package com.leaderboard.weeklyupdate;

public interface WeeklyLeaderboardResetService {

    /**
     * Clears the weekly points figure for every user that has stats, leaving every other
     * figure on the row untouched.
     *
     * @return the number of user-stats rows that were reset
     */
    int resetWeeklyLeaderboard();
}
