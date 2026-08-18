package com.challenge.q2;

import com.auth.filter.JWTFilter;
import com.auth.jwt.JWTService;
import com.auth.jwt.TokenBlacklistService;
import com.auth.service.UserDetailService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Black-box behaviour of an authenticated request: which tokens end up authenticating the
 * caller, and what the caller sees when a token is refused.
 */
@DisplayName("Q2 — what an authenticated request accepts")
class AuthenticatedRequestTest {

    private static final String PROTECTED_URI = "/api/auth/me";
    private static final String SSE_URI = "/api/notifications/stream";

    private JWTService jwtService;
    private TokenBlacklistService revocations;
    private JWTFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtService = new JWTService();
        revocations = new TokenBlacklistService();
        ReflectionTestUtils.setField(revocations, "jwtService", jwtService);

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

    private MockHttpServletResponse callWithBearerToken(String uri, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse callWithQueryParameterToken(String uri, String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setParameter("token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private String authenticatedName() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Mints a token signed with the application's own key, with an expiry of our choosing. */
    private String mintToken(String username, Instant issuedAt, Instant expiresAt) {
        String secret = (String) ReflectionTestUtils.getField(jwtService, "secretKey");
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("a freshly issued token authenticates the caller")
    void freshlyIssuedTokenAuthenticatesTheCaller() throws Exception {
        String token = jwtService.generateToken("alice");

        MockHttpServletResponse response = callWithBearerToken(PROTECTED_URI, token);

        assertThat(response.getStatus())
                .as("a valid, never-revoked token must not be refused")
                .isEqualTo(200);
        assertThat(authenticatedName())
                .as("a valid token must authenticate its subject")
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("a request with no token is left unauthenticated and passes through")
    void requestWithNoTokenIsLeftUnauthenticated() throws Exception {
        MockHttpServletResponse response = callWithBearerToken(PROTECTED_URI, null);

        assertThat(response.getStatus())
                .as("a request without a token must be passed on, not refused by the token check")
                .isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("no token means no authentication")
                .isNull();
    }

    @Test
    @DisplayName("a garbage token is refused with 401 and a JSON body")
    void garbageTokenIsRefusedWithUnauthorized() throws Exception {
        MockHttpServletResponse response = callWithBearerToken(PROTECTED_URI, "not-a-jwt-at-all");

        assertThat(response.getStatus())
                .as("an unparseable token must be refused with 401")
                .isEqualTo(401);
        assertThat(response.getContentAsString())
                .as("a refused token must come back as a JSON failure body")
                .contains("\"success\":false");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("a refused token must not authenticate anybody")
                .isNull();
    }

    @Test
    @DisplayName("an expired token is refused with 401")
    void expiredTokenIsRefusedWithUnauthorized() throws Exception {
        String expired = mintToken("alice",
                Instant.now().minusSeconds(36_000),
                Instant.now().minusSeconds(60));

        MockHttpServletResponse response = callWithBearerToken(PROTECTED_URI, expired);

        assertThat(response.getStatus())
                .as("a token past its expiry must be refused with 401")
                .isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("an expired token must not authenticate anybody")
                .isNull();
    }

    @Test
    @DisplayName("a revoked token is refused with 401 and never reaches the endpoint")
    void revokedTokenIsRefusedWithUnauthorized() throws Exception {
        String token = jwtService.generateToken("alice");
        revocations.blacklistToken(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", PROTECTED_URI);
        request.setRequestURI(PROTECTED_URI);
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus())
                .as("a revoked token must be refused with 401")
                .isEqualTo(401);
        assertThat(response.getContentAsString())
                .as("a refused token must come back as a JSON failure body")
                .contains("\"success\":false");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("a revoked token must not authenticate anybody")
                .isNull();
        assertThat(chain.getRequest())
                .as("a request carrying a revoked token must not be passed on to the endpoint")
                .isNull();
    }

    @Test
    @DisplayName("a revoked token stays refused on every later request")
    void revokedTokenStaysRefusedOnEveryLaterRequest() throws Exception {
        String token = jwtService.generateToken("alice");
        revocations.blacklistToken(token);

        for (int attempt = 1; attempt <= 3; attempt++) {
            SecurityContextHolder.clearContext();
            MockHttpServletResponse response = callWithBearerToken(PROTECTED_URI, token);

            assertThat(response.getStatus())
                    .as("request #%d with a revoked token must still be refused with 401", attempt)
                    .isEqualTo(401);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .as("request #%d with a revoked token must not authenticate anybody", attempt)
                    .isNull();
        }
    }

    @Test
    @DisplayName("revoking one session leaves the other session working")
    void revokingOneSessionLeavesTheOtherSessionWorking() throws Exception {
        String revokedToken = jwtService.generateToken("alice");
        String liveToken = jwtService.generateToken("bob");
        revocations.blacklistToken(revokedToken);

        MockHttpServletResponse refused = callWithBearerToken(PROTECTED_URI, revokedToken);
        assertThat(refused.getStatus())
                .as("the revoked session's token must be refused")
                .isEqualTo(401);

        SecurityContextHolder.clearContext();
        MockHttpServletResponse accepted = callWithBearerToken(PROTECTED_URI, liveToken);
        assertThat(accepted.getStatus())
                .as("the other session's token must keep working")
                .isEqualTo(200);
        assertThat(authenticatedName())
                .as("the other session must still authenticate as its own subject")
                .isEqualTo("bob");
    }

    @Test
    @DisplayName("the SSE endpoint accepts a live token supplied as a query parameter")
    void sseEndpointAcceptsLiveTokenAsQueryParameter() throws Exception {
        String token = jwtService.generateToken("alice");

        MockHttpServletResponse response = callWithQueryParameterToken(SSE_URI, token);

        assertThat(response.getStatus())
                .as("the SSE transport must keep working for a live token")
                .isEqualTo(200);
        assertThat(authenticatedName())
                .as("a live token in the query parameter must authenticate its subject")
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("the SSE endpoint refuses a revoked token supplied as a query parameter")
    void sseEndpointRefusesRevokedTokenAsQueryParameter() throws Exception {
        String token = jwtService.generateToken("alice");
        revocations.blacklistToken(token);

        MockHttpServletResponse response = callWithQueryParameterToken(SSE_URI, token);

        assertThat(response.getStatus())
                .as("a revoked token must be refused on the query-parameter transport too")
                .isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("a revoked token must not authenticate anybody, whatever the transport")
                .isNull();
    }
}
