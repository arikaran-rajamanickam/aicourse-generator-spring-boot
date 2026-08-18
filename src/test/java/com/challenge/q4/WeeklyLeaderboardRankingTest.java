package com.challenge.q4;

import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.leaderboard.dto.LeaderboardResponseDTO;
import com.leaderboard.dto.PagedLeaderboardDTO;
import com.leaderboard.model.UserStats;
import com.leaderboard.repository.UserStatsRepository;
import com.leaderboard.service.impl.WeeklyLeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Q4 — weekly leaderboard: ordering, tie-breaking, rank numbering and pagination.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeeklyLeaderboardRankingTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private UserRepo userRepo;

    private WeeklyLeaderboardService weeklyLeaderboardService;

    @BeforeEach
    void setUp() {
        weeklyLeaderboardService = new WeeklyLeaderboardService(userStatsRepository, userRepo);
        // Every user id used below resolves to a plausible account unless a test overrides it.
        when(userRepo.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(WeeklyLeaderboardFixtures.user(id, "user" + id, "User " + id));
        });
    }

    private void givenWeeklyRows(UserStats... rows) {
        when(userStatsRepository.findAllOrderByWeeklyPoints())
                .thenReturn(WeeklyLeaderboardFixtures.mutable(rows));
    }

    @Test
    @DisplayName("the board is ordered by weekly points, highest first, regardless of the order the rows arrive in")
    void ordersByWeeklyPointsDescending() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 5000, 10),
                WeeklyLeaderboardFixtures.stats(2L, 120, 90),
                WeeklyLeaderboardFixtures.stats(3L, 300, 50));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(0, 10);

        assertThat(result.getData())
                .as("rows must be ordered by weekly points descending, not by lifetime total "
                        + "and not by whatever order the repository happened to return")
                .extracting(LeaderboardResponseDTO::getUserId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("users on equal weekly points are tie-broken by ascending user id")
    void breaksTiesByUserIdAscending() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(7L, 100, 50),
                WeeklyLeaderboardFixtures.stats(3L, 9000, 50),
                WeeklyLeaderboardFixtures.stats(5L, 51, 50));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(0, 10);

        assertThat(result.getData())
                .as("equal weekly scores must produce a deterministic order: ascending user id")
                .extracting(LeaderboardResponseDTO::getUserId)
                .containsExactly(3L, 5L, 7L);
        assertThat(result.getData())
                .as("ranks are still 1..n even when the scores are tied")
                .extracting(LeaderboardResponseDTO::getRank)
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("the first page is ranked 1..size")
    void ranksFirstPageFromOne() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50),
                WeeklyLeaderboardFixtures.stats(3L, 40, 40));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(0, 3);

        assertThat(result.getData())
                .extracting(LeaderboardResponseDTO::getRank)
                .as("first page ranks start at 1")
                .containsExactly(1, 2, 3);
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("ranks continue across page boundaries: page 1 of size 3 is ranked 4, 5, 6")
    void ranksSecondPageWithOffset() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50),
                WeeklyLeaderboardFixtures.stats(3L, 40, 40),
                WeeklyLeaderboardFixtures.stats(4L, 30, 30),
                WeeklyLeaderboardFixtures.stats(5L, 20, 20),
                WeeklyLeaderboardFixtures.stats(6L, 10, 10));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(1, 3);

        assertThat(result.getData())
                .extracting(LeaderboardResponseDTO::getUserId)
                .as("page 1 of size 3 holds the 4th, 5th and 6th users")
                .containsExactly(4L, 5L, 6L);
        assertThat(result.getData())
                .extracting(LeaderboardResponseDTO::getRank)
                .as("rank must be the absolute position on the board, not the index within the page")
                .containsExactly(4, 5, 6);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(6L);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("ranks continue across page boundaries on a partial last page")
    void ranksPartialLastPageWithOffset() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50),
                WeeklyLeaderboardFixtures.stats(3L, 40, 40),
                WeeklyLeaderboardFixtures.stats(4L, 30, 30),
                WeeklyLeaderboardFixtures.stats(5L, 20, 20));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(2, 2);

        assertThat(result.getData())
                .extracting(LeaderboardResponseDTO::getUserId)
                .as("the last page holds the single remaining user")
                .containsExactly(5L);
        assertThat(result.getData())
                .extracting(LeaderboardResponseDTO::getRank)
                .as("the only row on page 2 of size 2 is rank 5")
                .containsExactly(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("a page past the end of the board is empty but still reports the real totals")
    void outOfRangePageIsEmpty() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50),
                WeeklyLeaderboardFixtures.stats(3L, 40, 40));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(9, 3);

        assertThat(result.getData()).as("no rows exist on page 9").isEmpty();
        assertThat(result.getPage()).as("the requested page is echoed back").isEqualTo(9);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).as("totals describe the board, not the page").isEqualTo(3L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("a page size larger than the board returns every row on one page")
    void pageSizeLargerThanBoard() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50));

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(0, 500);

        assertThat(result.getData()).hasSize(2);
        assertThat(result.getSize()).as("the requested size is echoed back unchanged").isEqualTo(500);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("an empty board is an empty page with zero totals, not an error")
    void emptyBoard() {
        givenWeeklyRows();

        PagedLeaderboardDTO result = weeklyLeaderboardService.getTopWeeklyUsers(0, 10);

        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).as("no rows means no pages").isZero();
    }

    @Test
    @DisplayName("a negative page number is rejected")
    void rejectsNegativePage() {
        assertThatThrownBy(() -> weeklyLeaderboardService.getTopWeeklyUsers(-1, 10))
                .as("page must be >= 0")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a page size below one is rejected")
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> weeklyLeaderboardService.getTopWeeklyUsers(0, 0))
                .as("size must be >= 1")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> weeklyLeaderboardService.getTopWeeklyUsers(0, -5))
                .as("size must be >= 1")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("each row carries the weekly score, the streak and the created-course count")
    void rowsCarryTheWeeklyScoreAndCounters() {
        givenWeeklyRows(WeeklyLeaderboardFixtures.richStats(
                1L, 5000, 120, 7, 4, 30, 2, 1, null));

        LeaderboardResponseDTO row = weeklyLeaderboardService.getTopWeeklyUsers(0, 10).getData().get(0);

        assertThat(row.getWeeklyPoints()).as("weeklyPoints holds the weekly figure").isEqualTo(120);
        assertThat(row.getTotalPoints())
                .as("totalPoints is the shared score slot of the DTO and on the weekly board "
                        + "it holds the score the board is ranked by, i.e. the weekly figure")
                .isEqualTo(120);
        assertThat(row.getCurrentStreak()).isEqualTo(7);
        assertThat(row.getCourseCount()).as("courseCount comes from totalCoursesCreated").isEqualTo(2);
        assertThat(row.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("display name and handle are hydrated from the user account")
    void hydratesDisplayNameAndHandle() {
        givenWeeklyRows(
                WeeklyLeaderboardFixtures.stats(1L, 60, 60),
                WeeklyLeaderboardFixtures.stats(2L, 50, 50),
                WeeklyLeaderboardFixtures.stats(3L, 40, 40));
        when(userRepo.findById(1L)).thenReturn(Optional.of(
                WeeklyLeaderboardFixtures.user(1L, "ada", "Ada Lovelace")));
        when(userRepo.findById(2L)).thenReturn(Optional.of(
                WeeklyLeaderboardFixtures.user(2L, "grace", "   ")));
        when(userRepo.findById(3L)).thenReturn(Optional.empty());

        List<LeaderboardResponseDTO> rows = weeklyLeaderboardService.getTopWeeklyUsers(0, 10).getData();

        assertThat(rows.get(0).getDisplayName()).as("display name wins when present").isEqualTo("Ada Lovelace");
        assertThat(rows.get(0).getHandle()).as("handle is the username").isEqualTo("ada");
        assertThat(rows.get(0).getUsername()).as("username mirrors the resolved display name").isEqualTo("Ada Lovelace");
        assertThat(rows.get(1).getDisplayName()).as("a blank display name falls back to the username").isEqualTo("grace");
        assertThat(rows.get(1).getHandle()).isEqualTo("grace");
        assertThat(rows.get(2).getDisplayName()).as("a missing account leaves the name unresolved").isNull();
    }

    @Test
    @DisplayName("the global leaderboard is untouched: it is still ordered by lifetime points")
    void doesNotDisturbTheGlobalBoard() {
        Users ada = WeeklyLeaderboardFixtures.user(1L, "ada", "Ada");
        when(userRepo.findById(1L)).thenReturn(Optional.of(ada));
        when(userStatsRepository.findAllOrderByTotalPoints()).thenReturn(WeeklyLeaderboardFixtures.mutable(
                WeeklyLeaderboardFixtures.stats(1L, 5000, 1)));

        com.leaderboard.service.impl.GlobalLeaderboardService global =
                new com.leaderboard.service.impl.GlobalLeaderboardService(userStatsRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(global, "userRepo", userRepo);

        LeaderboardResponseDTO row = global.getTopGlobalUsers(0, 10).getData().get(0);

        assertThat(row.getTotalPoints())
                .as("the global board must keep reporting the lifetime total as the score")
                .isEqualTo(5000);
        assertThat(row.getWeeklyPoints()).isEqualTo(1);
        assertThat(row.getRank()).isEqualTo(1);
    }
}
