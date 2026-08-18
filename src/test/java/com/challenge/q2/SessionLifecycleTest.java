package com.challenge.q2;

import com.auth.filter.JWTFilter;
import com.auth.jwt.JWTService;
import com.auth.jwt.RevocationLogoutHandler;
import com.auth.jwt.TokenBlacklistService;
import com.auth.service.UserDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The whole session lifecycle wired together the way the application wires it: log in, use the
 * token, log out, and try to use the token again.
 */
@DisplayName("Q2 — session lifecycle: login, use, logout, replay")
class SessionLifecycleTest {

    private static final String PROTECTED_URI = "/api/auth/me";
    private static final String SSE_URI = "/api/notifications/stream";

    private JWTService jwtService;
    private TokenBlacklistService revocations;
    private RevocationLogoutHandler logoutHandler;
    private JWTFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = new JWTService();

        revocations = new TokenBlacklistService();
        ReflectionTestUtils.setField(revocations, "jwtService", jwtService);

        logoutHandler = new RevocationLogoutHandler();
        ReflectionTestUtils.setField(logoutHandler, "blacklistService", revocations);

        UserDetailService userDetailService = mock(UserDetailService.class);
        when(userDetailService.loadUserByUsername(anyString())).thenAnswer(invocation -> User
                .withUsername(invocation.getArgument(0, String.class))
                .password("irrelevant-for-token-auth")
                .authorities("ROLE_USER")
                .build());

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(UserDetailService.class)).thenReturn(userDetailService);

        filter = new JWTFilter();
        ReflectionTestUtils.setField(filter, "jwtService", jwtService);
        ReflectionTestUtils.setField(filter, "tokenBlacklistService", revocations);
        ReflectionTestUtils.setField(filter, "context", context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** GET a protected endpoint with the token in the Authorization header. */
    private MockHttpServletResponse getWithBearerToken(String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PROTECTED_URI);
        request.setRequestURI(PROTECTED_URI);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    /** GET the SSE endpoint with the token in the query string. */
    private MockHttpServletResponse getStreamWithQueryToken(String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", SSE_URI);
        request.setRequestURI(SSE_URI);
        request.setParameter("token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    /** POST /api/auth/logout with the token in the Authorization header. */
    private void logoutWithBearerToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.setRequestURI("/api/auth/logout");
        request.addHeader("Authorization", "Bearer " + token);
        logoutHandler.logout(request, new MockHttpServletResponse(), null);
    }

    /** POST /api/auth/logout with the token in the query string. */
    private void logoutWithQueryToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.setRequestURI("/api/auth/logout");
        request.setParameter("token", token);
        logoutHandler.logout(request, new MockHttpServletResponse(), null);
    }

    @Test
    @DisplayName("a token works before logout and is refused after logout")
    void tokenWorksBeforeLogoutAndIsRefusedAfterLogout() throws Exception {
        String token = jwtService.generateToken("alice");

        assertThat(getWithBearerToken(token).getStatus())
                .as("before logout the token must be accepted")
                .isEqualTo(200);

        logoutWithBearerToken(token);

        assertThat(getWithBearerToken(token).getStatus())
                .as("after logout the same token must be refused with 401")
                .isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("after logout the replayed token must not authenticate anybody")
                .isNull();
    }

    @Test
    @DisplayName("a logged-out token is still refused on later requests")
    void loggedOutTokenIsStillRefusedOnLaterRequests() throws Exception {
        String token = jwtService.generateToken("alice");
        logoutWithBearerToken(token);

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(getWithBearerToken(token).getStatus())
                    .as("replay #%d of a logged-out token must be refused with 401", attempt)
                    .isEqualTo(401);
        }
    }

    @Test
    @DisplayName("logging out invalidates the token on the SSE transport as well")
    void loggingOutInvalidatesTheTokenOnTheSseTransportAsWell() throws Exception {
        String token = jwtService.generateToken("alice");

        assertThat(getStreamWithQueryToken(token).getStatus())
                .as("before logout the stream must accept the token")
                .isEqualTo(200);

        logoutWithBearerToken(token);

        assertThat(getStreamWithQueryToken(token).getStatus())
                .as("after logout the stream must refuse the same token")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("logging out with the token in the query string also ends the session")
    void loggingOutWithTheTokenInTheQueryStringAlsoEndsTheSession() throws Exception {
        String token = jwtService.generateToken("alice");

        logoutWithQueryToken(token);

        assertThat(getWithBearerToken(token).getStatus())
                .as("logout via the 'token' request parameter must end the session too")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("logging out one user does not log out another")
    void loggingOutOneUserDoesNotLogOutAnother() throws Exception {
        String aliceToken = jwtService.generateToken("alice");
        String bobToken = jwtService.generateToken("bob");

        logoutWithBearerToken(aliceToken);

        assertThat(getWithBearerToken(aliceToken).getStatus())
                .as("the user who logged out must be refused")
                .isEqualTo(401);
        assertThat(getWithBearerToken(bobToken).getStatus())
                .as("a user who did not log out must keep working")
                .isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .as("the user who did not log out must still authenticate as themselves")
                .isEqualTo("bob");
    }

    @Test
    @DisplayName("two logouts in a row leave both tokens refused")
    void twoLogoutsInARowLeaveBothTokensRefused() throws Exception {
        String firstToken = jwtService.generateToken("alice");
        String secondToken = jwtService.generateToken("bob");

        logoutWithBearerToken(firstToken);
        logoutWithBearerToken(secondToken);

        assertThat(getWithBearerToken(firstToken).getStatus())
                .as("the first logged-out token must stay refused after a second logout happens")
                .isEqualTo(401);
        assertThat(getWithBearerToken(secondToken).getStatus())
                .as("the second logged-out token must be refused as well")
                .isEqualTo(401);
    }

    /**
     * Two tokens issued for the same subject inside the same millisecond are byte-identical,
     * so keep asking until the clock has moved on.
     */
    private String issueTokenDistinctFrom(String username, String other) {
        String token = jwtService.generateToken(username);
        while (token.equals(other)) {
            token = jwtService.generateToken(username);
        }
        return token;
    }

    @Test
    @DisplayName("logging out does not stop a fresh login from working")
    void loggingOutDoesNotStopAFreshLoginFromWorking() throws Exception {
        String oldToken = jwtService.generateToken("alice");
        logoutWithBearerToken(oldToken);

        String newToken = issueTokenDistinctFrom("alice", oldToken);

        assertThat(getWithBearerToken(newToken).getStatus())
                .as("logging in again after logout must produce a token that works")
                .isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .as("the new session must authenticate the same user")
                .isEqualTo("alice");
        assertThat(getWithBearerToken(oldToken).getStatus())
                .as("logging in again must not resurrect the old, logged-out token")
                .isEqualTo(401);
    }
}
