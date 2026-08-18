package com.challenge.q4;

import com.leaderboard.config.CacheConfig;
import com.leaderboard.service.impl.WeeklyLeaderboardService;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardJob;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardResetService;
import com.leaderboard.weeklyupdate.impl.WeeklyLeaderboardResetServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Q4 — the weekly board is cached, the cache is dropped when the board is reset, and the reset
 * runs on a weekly schedule.
 *
 * Caching and scheduling are declarative, so they are asserted by reading the annotations and by
 * exercising the cache-manager bean directly. Nothing here starts a Spring context or waits on a
 * clock.
 */
class WeeklyLeaderboardCachingAndScheduleTest {

    private static final String WEEKLY_CACHE = "weeklyLeaderboard";
    private static final String GLOBAL_CACHE = "globalLeaderboard";

    private static Method weeklyReadMethod() throws Exception {
        return WeeklyLeaderboardService.class.getMethod("getTopWeeklyUsers", int.class, int.class);
    }

    private static Method resetMethod() throws Exception {
        return WeeklyLeaderboardResetServiceImpl.class.getMethod("resetWeeklyLeaderboard");
    }

    private static Method jobMethod() throws Exception {
        return WeeklyLeaderboardJob.class.getMethod("runWeeklyReset");
    }

    /** {@code value} and {@code cacheNames} are aliases; plain reflection only sees the one used. */
    private static Set<String> cacheNames(String[] value, String[] cacheNames) {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(java.util.Arrays.asList(value));
        names.addAll(java.util.Arrays.asList(cacheNames));
        return names;
    }

    @Test
    @DisplayName("the cache manager can hand out a weeklyLeaderboard cache without dropping globalLeaderboard")
    void cacheManagerKnowsTheWeeklyCache() {
        CacheManager cacheManager = new CacheConfig().cacheManager();

        assertThat(cacheManager.getCache(WEEKLY_CACHE))
                .as("the configured cache manager must be able to serve the '%s' cache; a Caffeine "
                        + "manager built with a fixed list of names refuses any name not on that list",
                        WEEKLY_CACHE)
                .isNotNull();
        assertThat(cacheManager.getCache(GLOBAL_CACHE))
                .as("the pre-existing '%s' cache must not be lost", GLOBAL_CACHE)
                .isNotNull();
    }

    @Test
    @DisplayName("the weekly board read is cached read-through under the weeklyLeaderboard cache")
    void weeklyReadIsCacheable() throws Exception {
        Cacheable cacheable = weeklyReadMethod().getAnnotation(Cacheable.class);

        assertThat(cacheable)
                .as("the weekly board read must be served from the cache manager that already exists; "
                        + "annotate WeeklyLeaderboardService.getTopWeeklyUsers with @Cacheable")
                .isNotNull();
        assertThat(cacheNames(cacheable.value(), cacheable.cacheNames()))
                .as("the weekly board must be cached under '%s'", WEEKLY_CACHE)
                .contains(WEEKLY_CACHE);
        assertThat(cacheable.key())
                .as("each page must be cached separately, so the key has to include page and size")
                .contains("page")
                .contains("size");
    }

    @Test
    @DisplayName("the reset evicts every entry of the weeklyLeaderboard cache")
    void resetEvictsTheWeeklyCache() throws Exception {
        CacheEvict cacheEvict = resetMethod().getAnnotation(CacheEvict.class);

        assertThat(cacheEvict)
                .as("a reset that leaves a stale board in the cache is not a reset; annotate "
                        + "WeeklyLeaderboardResetServiceImpl.resetWeeklyLeaderboard with @CacheEvict")
                .isNotNull();
        assertThat(cacheNames(cacheEvict.value(), cacheEvict.cacheNames()))
                .as("the eviction must target '%s'", WEEKLY_CACHE)
                .contains(WEEKLY_CACHE);
        assertThat(cacheEvict.allEntries())
                .as("every cached page is stale after a reset, not just one of them")
                .isTrue();
    }

    @Test
    @DisplayName("the reset job is scheduled once a week, on Monday")
    void resetJobIsScheduledWeeklyOnMonday() throws Exception {
        Scheduled scheduled = jobMethod().getAnnotation(Scheduled.class);

        assertThat(scheduled)
                .as("the weekly reset must run on a schedule; annotate "
                        + "WeeklyLeaderboardJob.runWeeklyReset with @Scheduled")
                .isNotNull();
        assertThat(scheduled.cron())
                .as("the schedule must be expressed as a cron expression, not a fixed delay")
                .isNotBlank();

        String[] fields = scheduled.cron().trim().split("\\s+");
        assertThat(fields)
                .as("a Spring cron expression has six fields: second minute hour day-of-month month day-of-week")
                .hasSize(6);
        assertThat(fields[5])
                .as("the week must roll over on Monday, so the day-of-week field must be MON (or 1)")
                .satisfiesAnyOf(
                        dayOfWeek -> assertThat(dayOfWeek).isEqualToIgnoringCase("MON"),
                        dayOfWeek -> assertThat(dayOfWeek).isEqualTo("1"));
    }

    @Test
    @DisplayName("the scheduled job delegates to the reset service instead of resetting things itself")
    void jobDelegatesToTheResetService() {
        WeeklyLeaderboardResetService resetService = mock(WeeklyLeaderboardResetService.class);

        new WeeklyLeaderboardJob(resetService).runWeeklyReset();

        verify(resetService)
                .resetWeeklyLeaderboard();
    }

    @Test
    @DisplayName("the job survives a failing reset so one bad week does not kill the scheduler")
    void jobSwallowsResetFailures() {
        WeeklyLeaderboardResetService resetService = mock(WeeklyLeaderboardResetService.class);
        org.mockito.Mockito.when(resetService.resetWeeklyLeaderboard())
                .thenThrow(new RuntimeException("deadlock detected"));

        WeeklyLeaderboardJob job = new WeeklyLeaderboardJob(resetService);

        org.assertj.core.api.Assertions.assertThatCode(job::runWeeklyReset)
                .as("a scheduled method that throws leaves nothing to retry and pollutes the logs "
                        + "with an unhandled error; log and move on")
                .doesNotThrowAnyException();
    }
}
