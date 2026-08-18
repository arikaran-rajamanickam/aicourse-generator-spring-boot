package com.challenge.q2;

import com.auth.jwt.JWTService;
import com.auth.jwt.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box behaviour of the server-side revocation record: once a token has been revoked it
 * must stay revoked until it would have expired on its own.
 */
@DisplayName("Q2 — server-side token revocation record")
class TokenRevocationRecordTest {

    private JWTService jwtService;
    private TokenBlacklistService revocations;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        revocations = new TokenBlacklistService();
        // production wires the JWT service into the revocation record by field injection
        ReflectionTestUtils.setField(revocations, "jwtService", jwtService);
    }

    @Test
    @DisplayName("a token that was never revoked is not reported as revoked")
    void tokenThatWasNeverRevokedIsNotReportedAsRevoked() {
        String token = jwtService.generateToken("alice");

        assertThat(revocations.isBlacklisted(token))
                .as("a freshly issued token that nobody logged out must not be treated as revoked")
                .isFalse();
    }

    @Test
    @DisplayName("a revoked token is reported as revoked on the very next check")
    void revokedTokenIsReportedAsRevokedImmediately() {
        String token = jwtService.generateToken("alice");

        revocations.blacklistToken(token);

        assertThat(revocations.isBlacklisted(token))
                .as("the token was just revoked, so the next check must report it as revoked")
                .isTrue();
    }

    @Test
    @DisplayName("a revoked token stays revoked across repeated checks")
    void revokedTokenStaysRevokedAcrossRepeatedChecks() {
        String token = jwtService.generateToken("alice");
        revocations.blacklistToken(token);

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(revocations.isBlacklisted(token))
                    .as("check #%d after revocation must still report the token as revoked", attempt)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("revoking a second token does not un-revoke the first")
    void revokingASecondTokenDoesNotUnRevokeTheFirst() {
        String first = jwtService.generateToken("alice");
        String second = jwtService.generateToken("bob");

        revocations.blacklistToken(first);
        revocations.blacklistToken(second);

        assertThat(revocations.isBlacklisted(first))
                .as("logging out a second session must not resurrect the first token")
                .isTrue();
        assertThat(revocations.isBlacklisted(second))
                .as("the token revoked most recently must be reported as revoked")
                .isTrue();
    }

    @Test
    @DisplayName("revoking many tokens keeps every one of them revoked")
    void revokingManyTokensKeepsEveryOneOfThemRevoked() {
        String[] tokens = new String[6];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = jwtService.generateToken("user-" + i);
            revocations.blacklistToken(tokens[i]);
        }

        for (int i = 0; i < tokens.length; i++) {
            assertThat(revocations.isBlacklisted(tokens[i]))
                    .as("token #%d was revoked and must still be reported as revoked", i)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("null and blank tokens are ignored rather than revoked")
    void nullAndBlankTokensAreIgnored() {
        revocations.blacklistToken(null);
        revocations.blacklistToken("");
        revocations.blacklistToken("   ");

        assertThat(revocations.isBlacklisted(null))
                .as("a null token must never be reported as revoked")
                .isFalse();
        assertThat(revocations.isBlacklisted(""))
                .as("an empty token must never be reported as revoked")
                .isFalse();
        assertThat(revocations.isBlacklisted("   "))
                .as("a blank token must never be reported as revoked")
                .isFalse();
    }

    @Test
    @DisplayName("revoking one token does not revoke a different, still-live token")
    void revokingOneTokenDoesNotRevokeADifferentToken() {
        String revoked = jwtService.generateToken("alice");
        String stillLive = jwtService.generateToken("bob");

        revocations.blacklistToken(revoked);

        assertThat(revocations.isBlacklisted(stillLive))
                .as("only the token that was logged out may be treated as revoked")
                .isFalse();
    }

    @Test
    @DisplayName("the revocation record works even when no JWT service is available to it")
    void revocationWorksWithoutTheOptionalJwtServiceCollaborator() {
        TokenBlacklistService standalone = new TokenBlacklistService();
        String token = jwtService.generateToken("alice");

        standalone.blacklistToken(token);

        assertThat(standalone.isBlacklisted(token))
                .as("revocation must not depend on the optional JWT service collaborator")
                .isTrue();
    }
}
