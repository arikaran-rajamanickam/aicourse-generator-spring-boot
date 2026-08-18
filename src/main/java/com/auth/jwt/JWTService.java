package com.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class JWTService {

    private static final Logger LOGGER = Logger.getLogger(JWTService.class.getName());

    // Use a static key for development to avoid issues with restarts invalidating
    // tokens
    private final String secretKey = "MySuperSecretKeyForJWTTokenGenerationWhichShouldBeLongEnough";

    public JWTService() {
        // No dynamic generation
    }

    public String generateToken(String username) {
        LOGGER.log(Level.FINE, "Generating JWT token for user: {0}", new Object[]{username});
        Map<String, Object> claims = new HashMap<>();

        return Jwts
                .builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .and()
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        // extract the username from jwt token
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        boolean isValid = (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
        if (!isValid) {
            LOGGER.log(Level.WARNING, "JWT Token validation failed for user: {0}",
                    new Object[]{userDetails.getUsername()});
        }
        return isValid;
    }

    private boolean isTokenExpired(String token) {
        boolean expired = extractExpiration(token).before(new Date());
        if (expired) {
            LOGGER.log(Level.WARNING, "JWT Token is expired.");
        }
        return expired;
    }

    // changed visibility to public so blacklist service can read expiry
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
