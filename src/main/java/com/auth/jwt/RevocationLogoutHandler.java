package com.auth.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logout handler that revokes (blacklists) incoming JWT found in the Authorization header
 * or as a request parameter named "token". Keeps behavior minimal so frontend can call
 * /api/auth/logout with Authorization header to revoke tokens.
 */
@Component
public class RevocationLogoutHandler implements LogoutHandler {

    private static final Logger LOGGER = Logger.getLogger(RevocationLogoutHandler.class.getName());

    @Autowired
    private TokenBlacklistService blacklistService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(8).trim();
            }

            if ((token == null || token.isBlank())) {
                // try request param
                token = request.getParameter("token");
            }

            if (token != null && !token.isBlank()) {
                blacklistService.blacklistToken(token);
                LOGGER.log(Level.INFO, "RevocationLogoutHandler: token blacklisted");
            } else {
                LOGGER.log(Level.FINE, "RevocationLogoutHandler: no token found to blacklist");
            }

            // clear context is handled by Spring Security; nothing else needed here
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "RevocationLogoutHandler: error while blacklisting token: {0}", new Object[]{e.getMessage()});
        }
    }
}

