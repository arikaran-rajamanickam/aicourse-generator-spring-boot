package com.auth.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory blacklist for revoked JWTs.
 * Stores token -> expirationMillis and removes entries once expired when checked.
 * This is intentionally lightweight. For production use a distributed store (Redis) is recommended.
 */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JWTService jwtService; // optional, used to read real expiration when available

    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) return;

        long expiryMillis = System.currentTimeMillis() + (1000L * 60 * 60 * 10); // fallback 10 hours
        try {
            if (jwtService != null) {
                Date exp = jwtService.extractExpiration(token);
                if (exp != null) expiryMillis = exp.getTime();
            }
        } catch (Exception ignored) {
            // fallback remains
        }

        blacklist.put(token, expiryMillis);
        // opportunistic cleanup
        cleanupExpired();
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : blacklist.entrySet()) {
            if (e.getValue() > now) {
                blacklist.remove(e.getKey());
            }
        }
    }
}

