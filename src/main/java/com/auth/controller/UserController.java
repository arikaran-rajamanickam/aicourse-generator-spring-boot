package com.auth.controller;

import com.auth.dto.LoginResponse;
import com.auth.dto.UserResponse;
import com.auth.jwt.TokenBlacklistService;
import com.auth.model.UserPrincipal;
import com.auth.model.Users;
import com.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5174", "http://127.0.0.1:5174"})
public class UserController {

    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService service;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registerUser(@RequestBody Users user) {
        LOGGER.log(Level.INFO, "Request received to register user: {0}", new Object[]{user.getUsername()});
        try {
            Users registeredUser = service.registerUser(user);
            LOGGER.log(Level.INFO, "User registered successfully: {0}", new Object[]{registeredUser.getUsername()});
            LoginResponse response = service.issueLoginResponse(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering user: {0}: {1}",
                    new Object[]{user.getUsername(), e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/login")
    public LoginResponse verifyUser(@RequestBody Users user) {
        LOGGER.log(Level.INFO, "Login request received for user: {0}", new Object[]{user.getUsername()});
        try {
            LoginResponse response = service.verify(user); // returns JWT
            LOGGER.log(Level.INFO, "User logged in successfully: {0}", new Object[]{user.getUsername()});
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error logging in user: {0}: {1}",
                    new Object[]{user.getUsername(), e.getMessage()});
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            }
            if ((token == null || token.isBlank())) {
                token = request.getParameter("token");
            }

            if (token != null && !token.isBlank()) {
                tokenBlacklistService.blacklistToken(token);
                LOGGER.log(Level.INFO, "UserController.logout: token blacklisted");
            } else {
                LOGGER.log(Level.FINE, "UserController.logout: no token provided to blacklist");
            }

            // Invalidate session if present
            try {
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error invalidating session during logout: {0}", new Object[]{e.getMessage()});
            }

            // Remove JSESSIONID cookie in response
            Cookie cookie = new Cookie("JSESSIONID", "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Logged out successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Logout error: {0}", new Object[]{e.getMessage()});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Logout failed"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        LOGGER.log(Level.FINE, "Request received for /me endpoint");
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to /me");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Users user = principal.getUser();

        LOGGER.log(Level.INFO, "Fetched details for currently authenticated user: {0}", new Object[]{user.getUsername()});
        return ResponseEntity.ok(new UserResponse(user));
    }
}
