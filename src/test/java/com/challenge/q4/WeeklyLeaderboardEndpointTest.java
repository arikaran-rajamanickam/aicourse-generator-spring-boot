package com.challenge.q4;

import com.auth.enums.UserRole;
import com.leaderboard.controller.LeaderboardController;
import com.leaderboard.dto.PagedLeaderboardDTO;
import com.leaderboard.service.impl.GlobalLeaderboardService;
import com.leaderboard.service.impl.WeeklyLeaderboardService;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Q4 — the HTTP contract of the weekly board and of the on-demand reset trigger.
 *
 * The controller is exercised directly with mocked collaborators; the request mapping itself
 * is asserted by reading the annotations, because no Spring context is started here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeeklyLeaderboardEndpointTest {

    @Mock
    private GlobalLeaderboardService globalLeaderboardService;

    @Mock
    private WeeklyLeaderboardService weeklyLeaderboardService;

    @Mock
    private WeeklyLeaderboardResetService weeklyLeaderboardResetService;

    @InjectMocks
    private LeaderboardController controller;

    private static Method weeklyBoardMethod() throws Exception {
        return LeaderboardController.class.getMethod("getWeeklyLeaderboard", int.class, int.class);
    }

    private static Method resetMethod() throws Exception {
        return LeaderboardController.class.getMethod("resetWeeklyLeaderboard", Authentication.class);
    }

    private static PagedLeaderboardDTO emptyPage(int page, int size) {
        return new PagedLeaderboardDTO(List.of(), page, size, 0L, 0);
    }

    @Test
    @DisplayName("the weekly board is exposed as GET /api/leaderboard/weekly with page=0 and size=10 defaults")
    void weeklyBoardIsMappedWithDefaults() throws Exception {
        RequestMapping classMapping = LeaderboardController.class.getAnnotation(RequestMapping.class);
        assertThat(classMapping).isNotNull();
        assertThat(classMapping.value()).as("the controller is still rooted at /api/leaderboard")
                .contains("/api/leaderboard");

        GetMapping mapping = weeklyBoardMethod().getAnnotation(GetMapping.class);
        assertThat(mapping).as("the weekly board must be a GET mapping").isNotNull();
        assertThat(mapping.value()).as("the weekly board lives at /weekly").contains("/weekly");

        Annotation[][] parameterAnnotations = weeklyBoardMethod().getParameterAnnotations();
        assertThat(requestParamDefault(parameterAnnotations[0])).as("page defaults to 0").isEqualTo("0");
        assertThat(requestParamDefault(parameterAnnotations[1])).as("size defaults to 10").isEqualTo("10");
    }

    private static String requestParamDefault(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestParam requestParam) {
                return requestParam.defaultValue();
            }
        }
        return null;
    }

    @Test
    @DisplayName("the reset trigger is exposed as POST /api/leaderboard/weekly/reset")
    void resetTriggerIsMappedAsPost() throws Exception {
        PostMapping mapping = resetMethod().getAnnotation(PostMapping.class);
        assertThat(mapping).as("an operational reset must not be a GET").isNotNull();
        assertThat(mapping.value()).as("the reset trigger lives at /weekly/reset").contains("/weekly/reset");
    }

    @Test
    @DisplayName("GET weekly returns 200 and the page produced by the weekly service")
    void weeklyBoardReturnsThePage() {
        PagedLeaderboardDTO page = emptyPage(2, 5);
        when(weeklyLeaderboardService.getTopWeeklyUsers(2, 5)).thenReturn(page);

        ResponseEntity<?> response = controller.getWeeklyLeaderboard(2, 5);

        assertThat(response.getStatusCode().value()).as("a successful read is 200").isEqualTo(200);
        assertThat(response.getBody()).as("the paged DTO is returned as-is").isSameAs(page);
        verify(weeklyLeaderboardService).getTopWeeklyUsers(2, 5);
    }

    @Test
    @DisplayName("GET weekly answers 400 when page or size is out of range")
    void weeklyBoardRejectsBadPagination() {
        when(weeklyLeaderboardService.getTopWeeklyUsers(anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("page must be >= 0 and size must be >= 1"));

        assertThat(controller.getWeeklyLeaderboard(-1, 10).getStatusCode().value())
                .as("a negative page is a client error, not a 500").isEqualTo(400);
        assertThat(controller.getWeeklyLeaderboard(0, 0).getStatusCode().value())
                .as("a size of 0 is a client error, not a 500").isEqualTo(400);
        assertThat(controller.getWeeklyLeaderboard(0, -3).getStatusCode().value())
                .as("a negative size is a client error, not a 500").isEqualTo(400);
    }

    @Test
    @DisplayName("GET weekly answers 500 when the weekly service blows up")
    void weeklyBoardReportsServerErrors() {
        when(weeklyLeaderboardService.getTopWeeklyUsers(0, 10))
                .thenThrow(new RuntimeException("connection reset"));

        ResponseEntity<?> response = controller.getWeeklyLeaderboard(0, 10);

        assertThat(response.getStatusCode().value())
                .as("an unexpected failure is a 500, and must not propagate out of the controller")
                .isEqualTo(500);
    }

    @Test
    @DisplayName("the reset trigger answers 401 when there is no authenticated caller")
    void resetRequiresAuthentication() {
        assertThat(controller.resetWeeklyLeaderboard(null).getStatusCode().value())
                .as("a missing Authentication is 401, matching /api/leaderboard/me")
                .isEqualTo(401);

        Authentication notAuthenticated = new UsernamePasswordAuthenticationToken("anonymous", null);
        assertThat(controller.resetWeeklyLeaderboard(notAuthenticated).getStatusCode().value())
                .as("an unauthenticated token is 401")
                .isEqualTo(401);

        verifyNoInteractions(weeklyLeaderboardResetService);
    }

    @Test
    @DisplayName("the reset trigger answers 403 for an authenticated non-admin caller")
    void resetIsAdminOnly() {
        Authentication user = WeeklyLeaderboardFixtures.authenticatedAs(UserRole.USER);
        Authentication premium = WeeklyLeaderboardFixtures.authenticatedAs(UserRole.PREMIUM_USER);

        assertThat(controller.resetWeeklyLeaderboard(user).getStatusCode().value())
                .as("an ordinary user must not be able to wipe everyone's weekly points")
                .isEqualTo(403);
        assertThat(controller.resetWeeklyLeaderboard(premium).getStatusCode().value())
                .as("a premium user is still not an admin")
                .isEqualTo(403);

        verifyNoInteractions(weeklyLeaderboardResetService);
    }

    @Test
    @DisplayName("the reset trigger answers 200 with the number of rows reset for an admin caller")
    void resetSucceedsForAdmin() {
        when(weeklyLeaderboardResetService.resetWeeklyLeaderboard()).thenReturn(4);

        ResponseEntity<?> response =
                controller.resetWeeklyLeaderboard(WeeklyLeaderboardFixtures.authenticatedAs(UserRole.ADMIN));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .as("the response body reports how many rows were reset under the key usersReset")
                .isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
                .as("usersReset must carry the count returned by the reset service")
                .containsEntry("usersReset", 4);
        verify(weeklyLeaderboardResetService).resetWeeklyLeaderboard();
    }

    @Test
    @DisplayName("the reset trigger answers 500 when the reset service blows up")
    void resetReportsServerErrors() {
        when(weeklyLeaderboardResetService.resetWeeklyLeaderboard())
                .thenThrow(new RuntimeException("deadlock detected"));

        ResponseEntity<?> response =
                controller.resetWeeklyLeaderboard(WeeklyLeaderboardFixtures.authenticatedAs(UserRole.ADMIN));

        assertThat(response.getStatusCode().value())
                .as("an unexpected failure is a 500, and must not propagate out of the controller")
                .isEqualTo(500);
    }

    @Test
    @DisplayName("the existing global endpoints are untouched")
    void globalEndpointsStillBehave() throws Exception {
        PagedLeaderboardDTO page = emptyPage(0, 10);
        when(globalLeaderboardService.getTopGlobalUsers(0, 10)).thenReturn(page);

        ResponseEntity<?> response = controller.getGlobalLeaderboard(0, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(page);
        assertThat(LeaderboardController.class.getMethod("getGlobalLeaderboard", int.class, int.class)
                .getAnnotation(GetMapping.class).value())
                .as("the global board must stay at /global")
                .contains("/global");
        assertThat(controller.getMyRank(null).getStatusCode().value())
                .as("/me still answers 401 without an authenticated caller")
                .isEqualTo(401);
    }
}
