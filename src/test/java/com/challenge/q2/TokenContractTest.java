package com.challenge.q2;

import com.auth.jwt.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The documented token contract: the username is the subject and the token is good for a fixed
 * ten hours. Fixing revocation must not change either of those.
 */
@DisplayName("Q2 — issued token contract")
class TokenContractTest {

    private static final Duration DOCUMENTED_LIFETIME = Duration.ofHours(10);

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
    }

    private static UserDetails userNamed(String username) {
        return User.withUsername(username).password("irrelevant").authorities("ROLE_USER").build();
    }

    @Test
    @DisplayName("an issued token carries the username as its subject")
    void issuedTokenCarriesTheUsernameAsItsSubject() {
        String token = jwtService.generateToken("alice");

        assertThat(jwtService.extractUserName(token))
                .as("the token subject is the contract the rest of auth reads the user from")
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("an issued token is good for the documented ten hours")
    void issuedTokenIsGoodForTheDocumentedTenHours() {
        long before = System.currentTimeMillis();
        String token = jwtService.generateToken("alice");
        long after = System.currentTimeMillis();

        long expiry = jwtService.extractExpiration(token).getTime();

        assertThat(expiry - after)
                .as("remaining lifetime of a just-issued token must be the documented 10 hours")
                .isLessThanOrEqualTo(DOCUMENTED_LIFETIME.toMillis());
        assertThat(expiry - before)
                .as("remaining lifetime of a just-issued token must not be cut short")
                .isGreaterThanOrEqualTo(DOCUMENTED_LIFETIME.toMillis() - Duration.ofSeconds(30).toMillis());
    }

    @Test
    @DisplayName("a live token validates for the user it was issued to")
    void liveTokenValidatesForTheUserItWasIssuedTo() {
        String token = jwtService.generateToken("alice");

        assertThat(jwtService.validateToken(token, userNamed("alice")))
                .as("a live token must validate against its own subject")
                .isTrue();
    }

    @Test
    @DisplayName("a token does not validate for a different user")
    void tokenDoesNotValidateForADifferentUser() {
        String token = jwtService.generateToken("alice");

        assertThat(jwtService.validateToken(token, userNamed("bob")))
                .as("a token must not validate against somebody else's account")
                .isFalse();
    }
}
