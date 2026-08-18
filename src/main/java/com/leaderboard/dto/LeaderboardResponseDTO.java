package com.leaderboard.dto;

public class LeaderboardResponseDTO extends RankScoreDTO {
    public LeaderboardResponseDTO(int rank, Long userId, int totalPoints, String displayName, String handle, int courseCount, int currentStreak, int weeklyPoints) {
        this.rank = rank;
        this.userId = userId;
        this.totalPoints = totalPoints;
        this.displayName = displayName;
        this.handle = handle;
        this.username = displayName;
        this.courseCount = courseCount;
        this.currentStreak = currentStreak;
        this.weeklyPoints = weeklyPoints;
    }
}