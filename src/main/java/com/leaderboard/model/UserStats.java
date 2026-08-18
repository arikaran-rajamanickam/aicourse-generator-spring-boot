package com.leaderboard.model;

import com.aicourse.utils.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "userstats")
public class UserStats {

    @Id
    @Column(name = "userstate_id", nullable = false, updatable = false)
    private Long userstateId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "weekly_points", nullable = false)
    private Integer weeklyPoints;

    @Column(name = "courses_completed", nullable = false)
    private Integer coursesCompleted;

    @Column(name = "lessons_completed", nullable = false)
    private Integer lessonsCompleted;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak;

    @Column(name = "total_courses_created", nullable = false)
    private Integer totalCoursesCreated;

    @Column(name = "total_projects_created", nullable = false)
    private Integer totalProjectsCreated;

    @Column(name = "last_activity_date")
    private java.time.LocalDate lastActivityDate;

    protected UserStats() {
    }

    public UserStats(Long userId) {
        this.userstateId = SnowflakeIdGenerator.generateId(); // generate immediately
        this.userId = userId;
        this.totalPoints = 0;
        this.weeklyPoints = 0;
        this.coursesCompleted = 0;
        this.lessonsCompleted = 0;
        this.currentStreak = 0;
        this.totalCoursesCreated = 0;
        this.totalProjectsCreated = 0;
        this.lastActivityDate = null;
    }

    public void addPoints(int points) {
        this.totalPoints += points;
        this.weeklyPoints += points;
    }

    public void incrementCoursesCompleted() {
        this.coursesCompleted++;
    }

    public void incrementLessonsCompleted() {
        this.lessonsCompleted++;
    }

    public void incrementStreak() {
        this.currentStreak++;
    }

    public void resetWeeklyPoints() {
        this.weeklyPoints = 0;
    }

    public void resetStreak() {
        this.currentStreak = 0;
    }

    public void incrementTotalCoursesCreated() {
        this.totalCoursesCreated++;
    }

    public void incrementTotalProjectsCreated() {
        this.totalProjectsCreated++;
    }

    public Long getUserstateId() {
        return userstateId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public Integer getWeeklyPoints() {
        return weeklyPoints;
    }

    public Integer getCoursesCompleted() {
        return coursesCompleted;
    }

    public Integer getLessonsCompleted() {
        return lessonsCompleted;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public Integer getTotalCoursesCreated() {
        return totalCoursesCreated;
    }

    public Integer getTotalProjectsCreated() {
        return totalProjectsCreated;
    }

    public java.time.LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(java.time.LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

}