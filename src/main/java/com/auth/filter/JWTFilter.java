package com.auth.filter;

import com.auth.jwt.JWTService;
import com.auth.jwt.TokenBlacklistService;
import com.auth.service.UserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JWTFilter.class.getName());

    @Autowired
    private JWTService jwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request.getRequestURI().contains("/api/notifications/stream")) {
            // Support token as query param for SSE (EventSource doesn't support headers)
            token = request.getParameter("token");
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        LOGGER.log(Level.FINE, "JWTFilter: Extracted Token: [{0}...]",
                new Object[]{token.substring(0, Math.min(token.length(), 10))});
        String username;

        try {
            username = jwtService.extractUserName(token);
        } catch (ExpiredJwtException e) {
            LOGGER.log(Level.INFO, "JWTFilter: Token expired: {0}", new Object[]{e.getMessage()});
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "JWT expired. Please login again.");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "JWTFilter: Invalid token: {0}", new Object[]{e.getMessage()});
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "Invalid JWT token.");
            return;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JWTFilter: Token processing error: {0}", new Object[]{e.getMessage()});
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "Authentication token error.");
            return;
        }

        // Check if the token was revoked/blacklisted
        try {
            if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(token)) {
                LOGGER.log(Level.WARNING, "JWTFilter: Token is blacklisted for user {0}", new Object[]{username});
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, "JWT token has been revoked. Please login again.");
                return;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "JWTFilter: Error checking blacklist: {0}", new Object[]{e.getMessage()});
            // proceed - blacklist failure should not crash auth
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails userDetails = context.getBean(UserDetailService.class).loadUserByUsername(username);

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    LOGGER.log(Level.FINE, "JWTFilter: Authentication successful for user {0}",
                            new Object[]{username});
                } else {
                    LOGGER.log(Level.WARNING, "JWTFilter: Token invalid for user {0}", new Object[]{username});
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "JWTFilter: Authentication failed for user {0}: {1}",
                        new Object[]{username, e.getMessage()});
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

}
