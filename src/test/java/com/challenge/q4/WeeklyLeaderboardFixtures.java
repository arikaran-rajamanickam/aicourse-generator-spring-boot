package com.challenge.q4;

import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.leaderboard.model.UserStats;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test fixtures for the Q4 weekly-leaderboard suite.
 *
 * {@link UserStats} exposes no setters, so every figure here is produced through the
 * entity's own public mutators.
 */
final class WeeklyLeaderboardFixtures {

    private WeeklyLeaderboardFixtures() {
    }

    /**
     * Builds a stats row whose lifetime total and weekly figure differ.
     *
     * @param totalPoints  lifetime total; must be >= weeklyPoints
     * @param weeklyPoints points earned in the current week
     */
    static UserStats stats(long userId, int totalPoints, int weeklyPoints) {
        if (totalPoints < weeklyPoints) {
            throw new IllegalArgumentException("totalPoints must be >= weeklyPoints");
        }
        UserStats stats = new UserStats(userId);
        stats.addPoints(totalPoints - weeklyPoints);
        stats.resetWeeklyPoints();
        stats.addPoints(weeklyPoints);
        return stats;
    }

    /**
     * Builds a fully populated stats row: lifetime total, weekly figure, streak, the four
     * activity counters and a last-activity date.
     */
    static UserStats richStats(long userId,
                               int totalPoints,
                               int weeklyPoints,
                               int currentStreak,
                               int coursesCompleted,
                               int lessonsCompleted,
                               int coursesCreated,
                               int projectsCreated,
                               LocalDate lastActivityDate) {
        UserStats stats = stats(userId, totalPoints, weeklyPoints);
        repeat(currentStreak, stats::incrementStreak);
        repeat(coursesCompleted, stats::incrementCoursesCompleted);
        repeat(lessonsCompleted, stats::incrementLessonsCompleted);
        repeat(coursesCreated, stats::incrementTotalCoursesCreated);
        repeat(projectsCreated, stats::incrementTotalProjectsCreated);
        stats.setLastActivityDate(lastActivityDate);
        return stats;
    }

    static Users user(long id, String username, String displayName) {
        Users user = new Users();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPassword("x");
        user.setRoles(UserRole.USER);
        return user;
    }

    static Authentication authenticatedAs(UserRole role) {
        Users user = user(99L, "someone", "Some One");
        user.setRoles(role);
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /** A mutable list — the reset is expected to mutate the entities it is handed. */
    static List<UserStats> mutable(UserStats... stats) {
        return new ArrayList<>(Arrays.asList(stats));
    }

    private static void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }
}
