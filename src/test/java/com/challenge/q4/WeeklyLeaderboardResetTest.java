package com.challenge.q4;

import com.leaderboard.model.UserStats;
import com.leaderboard.repository.UserStatsRepository;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardResetService;
import com.leaderboard.weeklyupdate.impl.WeeklyLeaderboardResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Q4 — the weekly reset must clear the weekly figure and nothing else.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyLeaderboardResetTest {

    private static final LocalDate LAST_ACTIVITY = LocalDate.of(2026, 3, 9);

    @Mock
    private UserStatsRepository userStatsRepository;

    private WeeklyLeaderboardResetService resetService;

    @BeforeEach
    void setUp() {
        resetService = new WeeklyLeaderboardResetServiceImpl(userStatsRepository);
    }

    private UserStats populated(long userId) {
        return WeeklyLeaderboardFixtures.richStats(userId, 5000, 120, 7, 4, 30, 2, 1, LAST_ACTIVITY);
    }

    @Test
    @DisplayName("the reset zeroes the weekly figure for every user")
    void zeroesEveryWeeklyFigure() {
        UserStats first = populated(1L);
        UserStats second = WeeklyLeaderboardFixtures.stats(2L, 80, 80);
        when(userStatsRepository.findAll()).thenReturn(WeeklyLeaderboardFixtures.mutable(first, second));

        int reset = resetService.resetWeeklyLeaderboard();

        assertThat(reset).as("the reset reports how many stats rows it cleared").isEqualTo(2);
        assertThat(first.getWeeklyPoints()).as("weekly points must be cleared").isZero();
        assertThat(second.getWeeklyPoints()).as("weekly points must be cleared").isZero();
    }

    @Test
    @DisplayName("the reset preserves the lifetime total, the streak, the counters and the last-activity date")
    void preservesEverythingExceptTheWeeklyFigure() {
        UserStats stats = populated(1L);
        when(userStatsRepository.findAll()).thenReturn(WeeklyLeaderboardFixtures.mutable(stats));

        resetService.resetWeeklyLeaderboard();

        assertThat(stats.getWeeklyPoints()).as("weekly points must be cleared").isZero();
        assertThat(stats.getTotalPoints()).as("the lifetime total must survive the reset").isEqualTo(5000);
        assertThat(stats.getCurrentStreak()).as("the streak must survive the reset").isEqualTo(7);
        assertThat(stats.getCoursesCompleted()).as("coursesCompleted must survive the reset").isEqualTo(4);
        assertThat(stats.getLessonsCompleted()).as("lessonsCompleted must survive the reset").isEqualTo(30);
        assertThat(stats.getTotalCoursesCreated()).as("totalCoursesCreated must survive the reset").isEqualTo(2);
        assertThat(stats.getTotalProjectsCreated()).as("totalProjectsCreated must survive the reset").isEqualTo(1);
        assertThat(stats.getLastActivityDate()).as("lastActivityDate must survive the reset").isEqualTo(LAST_ACTIVITY);
        assertThat(stats.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("the reset persists the rows it cleared")
    void persistsTheClearedRows() {
        UserStats first = populated(1L);
        UserStats second = populated(2L);
        when(userStatsRepository.findAll()).thenReturn(WeeklyLeaderboardFixtures.mutable(first, second));

        resetService.resetWeeklyLeaderboard();

        verify(userStatsRepository).saveAll(anyIterable());
    }

    @Test
    @DisplayName("running the reset twice is harmless and reports the same number of rows")
    void isIdempotent() {
        UserStats stats = populated(1L);
        List<UserStats> rows = WeeklyLeaderboardFixtures.mutable(stats);
        when(userStatsRepository.findAll()).thenReturn(rows);

        int first = resetService.resetWeeklyLeaderboard();
        int second = resetService.resetWeeklyLeaderboard();

        assertThat(first).isEqualTo(1);
        assertThat(second).as("a second reset over already-cleared rows still reports them").isEqualTo(1);
        assertThat(stats.getWeeklyPoints()).as("weekly points stay at zero").isZero();
        assertThat(stats.getTotalPoints()).as("a repeated reset must not erode the lifetime total").isEqualTo(5000);
        assertThat(stats.getCurrentStreak()).as("a repeated reset must not erode the streak").isEqualTo(7);
        verify(userStatsRepository, times(2)).saveAll(anyIterable());
    }

    @Test
    @DisplayName("resetting an empty board reports zero rows and does not fail")
    void handlesAnEmptyBoard() {
        when(userStatsRepository.findAll()).thenReturn(WeeklyLeaderboardFixtures.mutable());

        assertThat(resetService.resetWeeklyLeaderboard())
                .as("nothing to reset means zero rows reset")
                .isZero();
    }
}
