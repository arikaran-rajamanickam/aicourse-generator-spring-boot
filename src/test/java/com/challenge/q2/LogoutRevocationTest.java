package com.challenge.q2;

import com.auth.jwt.JWTService;
import com.auth.jwt.RevocationLogoutHandler;
import com.auth.jwt.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Black-box behaviour of logout: whichever way the client hands its token to
 * POST /api/auth/logout, that exact token has to end up revoked.
 */
@DisplayName("Q2 — logout revokes the caller's token")
class LogoutRevocationTest {

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JWTService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RevocationLogoutHandler logoutHandlerBackedBy(TokenBlacklistService revocations) {
        RevocationLogoutHandler handler = new RevocationLogoutHandler();
        ReflectionTestUtils.setField(handler, "blacklistService", revocations);
        return handler;
    }

    private MockHttpServletRequest logoutRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.setRequestURI("/api/auth/logout");
        return request;
    }

    @Test
    @DisplayName("logout with an Authorization header revokes that token")
    void logoutWithAuthorizationHeaderRevokesThatToken() {
        TokenBlacklistService revocations = new TokenBlacklistService();
        ReflectionTestUtils.setField(revocations, "jwtService", jwtService);
        String token = jwtService.generateToken("alice");

        MockHttpServletRequest request = logoutRequest();
        request.addHeader("Authorization", "Bearer " + token);

        logoutHandlerBackedBy(revocations).logout(request, new MockHttpServletResponse(), null);

        assertThat(revocations.isBlacklisted(token))
                .as("after logging out with 'Authorization: Bearer <token>' that token must be revoked")
                .isTrue();
    }

    @Test
    @DisplayName("logout with the token as a request parameter revokes that token")
    void logoutWithTokenRequestParameterRevokesThatToken() {
        TokenBlacklistService revocations = new TokenBlacklistService();
        ReflectionTestUtils.setField(revocations, "jwtService", jwtService);
        String token = jwtService.generateToken("alice");

        MockHttpServletRequest request = logoutRequest();
        request.setParameter("token", token);

        logoutHandlerBackedBy(revocations).logout(request, new MockHttpServletResponse(), null);

        assertThat(revocations.isBlacklisted(token))
                .as("the SSE client logs out with ?token=<token>, so that token must be revoked too")
                .isTrue();
    }

    @Test
    @DisplayName("logout revokes exactly the token the client sent in the header")
    void logoutRevokesExactlyTheTokenSentInTheHeader() {
        TokenBlacklistService revocations = mock(TokenBlacklistService.class);
        String token = jwtService.generateToken("alice");

        MockHttpServletRequest request = logoutRequest();
        request.addHeader("Authorization", "Bearer " + token);

        logoutHandlerBackedBy(revocations).logout(request, new MockHttpServletResponse(), null);

        ArgumentCaptor<String> revoked = ArgumentCaptor.forClass(String.class);
        verify(revocations).blacklistToken(revoked.capture());
        assertThat(revoked.getValue())
                .as("the revoked token must be byte-for-byte the token the client presented")
                .isEqualTo(token);
    }

    @Test
    @DisplayName("logout revokes exactly the token the client sent as a request parameter")
    void logoutRevokesExactlyTheTokenSentAsRequestParameter() {
        TokenBlacklistService revocations = mock(TokenBlacklistService.class);
        String token = jwtService.generateToken("alice");

        MockHttpServletRequest request = logoutRequest();
        request.setParameter("token", token);

        logoutHandlerBackedBy(revocations).logout(request, new MockHttpServletResponse(), null);

        ArgumentCaptor<String> revoked = ArgumentCaptor.forClass(String.class);
        verify(revocations).blacklistToken(revoked.capture());
        assertThat(revoked.getValue())
                .as("the revoked token must be byte-for-byte the token the client presented")
                .isEqualTo(token);
    }

    @Test
    @DisplayName("logout revokes the same token no matter which transport carried it")
    void logoutRevokesTheSameTokenWhicheverTransportCarriedIt() {
        String token = jwtService.generateToken("alice");

        TokenBlacklistService viaHeader = mock(TokenBlacklistService.class);
        MockHttpServletRequest headerRequest = logoutRequest();
        headerRequest.addHeader("Authorization", "Bearer " + token);
        logoutHandlerBackedBy(viaHeader).logout(headerRequest, new MockHttpServletResponse(), null);

        TokenBlacklistService viaParameter = mock(TokenBlacklistService.class);
        MockHttpServletRequest parameterRequest = logoutRequest();
        parameterRequest.setParameter("token", token);
        logoutHandlerBackedBy(viaParameter).logout(parameterRequest, new MockHttpServletResponse(), null);

        ArgumentCaptor<String> fromHeader = ArgumentCaptor.forClass(String.class);
        verify(viaHeader).blacklistToken(fromHeader.capture());
        ArgumentCaptor<String> fromParameter = ArgumentCaptor.forClass(String.class);
        verify(viaParameter).blacklistToken(fromParameter.capture());

        assertThat(fromHeader.getValue())
                .as("header logout and query-parameter logout must revoke the identical token")
                .isEqualTo(fromParameter.getValue());
    }

    @Test
    @DisplayName("logout without any token revokes nothing and does not blow up")
    void logoutWithoutAnyTokenRevokesNothing() {
        TokenBlacklistService revocations = mock(TokenBlacklistService.class);

        logoutHandlerBackedBy(revocations).logout(logoutRequest(), new MockHttpServletResponse(), null);

        verify(revocations, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("logout with a non-bearer Authorization header revokes nothing")
    void logoutWithNonBearerAuthorizationHeaderRevokesNothing() {
        TokenBlacklistService revocations = mock(TokenBlacklistService.class);

        MockHttpServletRequest request = logoutRequest();
        request.addHeader("Authorization", "Basic YWxpY2U6c2VjcmV0");

        logoutHandlerBackedBy(revocations).logout(request, new MockHttpServletResponse(), null);

        verify(revocations, never()).blacklistToken(anyString());
    }
}
