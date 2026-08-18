package com.about.pojo;

import com.auth.enums.UserRole;

import java.time.OffsetDateTime;

public class ProfileResponsePojo {

    private Long id;
    private String username;
    private String displayName;
    private UserRole role;
    private OffsetDateTime createdAt;
    private StatsSnapshot stats;
    private String token;

    public ProfileResponsePojo() {
    }

    public ProfileResponsePojo(Long id, String username, String displayName, UserRole role,
                               OffsetDateTime createdAt, StatsSnapshot stats) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = createdAt;
        this.stats = stats;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHandle() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public StatsSnapshot getStats() {
        return stats;
    }

    public void setStats(StatsSnapshot stats) {
        this.stats = stats;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static class StatsSnapshot {

        private int totalPoints;
        private int weeklyPoints;
        private int coursesCompleted;
        private int lessonsCompleted;
        private int currentStreak;

        public StatsSnapshot() {
        }

        public StatsSnapshot(int totalPoints, int weeklyPoints,
                             int coursesCompleted, int lessonsCompleted,
                             int currentStreak) {
            this.totalPoints = totalPoints;
            this.weeklyPoints = weeklyPoints;
            this.coursesCompleted = coursesCompleted;
            this.lessonsCompleted = lessonsCompleted;
            this.currentStreak = currentStreak;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public void setTotalPoints(int totalPoints) {
            this.totalPoints = totalPoints;
        }

        public int getWeeklyPoints() {
            return weeklyPoints;
        }

        public void setWeeklyPoints(int weeklyPoints) {
            this.weeklyPoints = weeklyPoints;
        }

        public int getCoursesCompleted() {
            return coursesCompleted;
        }

        public void setCoursesCompleted(int coursesCompleted) {
            this.coursesCompleted = coursesCompleted;
        }

        public int getLessonsCompleted() {
            return lessonsCompleted;
        }

        public void setLessonsCompleted(int lessonsCompleted) {
            this.lessonsCompleted = lessonsCompleted;
        }

        public int getCurrentStreak() {
            return currentStreak;
        }

        public void setCurrentStreak(int currentStreak) {
            this.currentStreak = currentStreak;
        }
    }
}