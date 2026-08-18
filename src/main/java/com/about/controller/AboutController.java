package com.about.controller;

import com.about.pojo.ChangePasswordRequestPojo;
import com.about.pojo.ProfileResponsePojo;
import com.about.pojo.UpdateProfileRequestPojo;
import com.about.service.AboutService;
import com.auth.jwt.JWTService;
import com.auth.model.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/about")
public class AboutController {

    private static final Logger LOGGER = Logger.getLogger(AboutController.class.getName());

    @Autowired
    private AboutService aboutService;

    @Autowired
    private JWTService jwtService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ProfileResponsePojo profile = aboutService.getProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch profile for userId {0}: {1}",
                    new Object[]{userId, e.getMessage()});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch profile.");
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequestPojo request) {

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            ProfileResponsePojo updated = aboutService.updateProfile(userId, request);

            // Generate a fresh JWT with the updated handle so the user doesn't get logged out
            String newToken = jwtService.generateToken(updated.getHandle());
            updated.setToken(newToken);

            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            // Validation errors — send back to client as 400
            LOGGER.log(Level.WARNING, "Profile update validation failed for userId {0}: {1}",
                    new Object[]{userId, e.getMessage()});
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update profile for userId {0}: {1}",
                    new Object[]{userId, e.getMessage()});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update profile.");
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequestPojo request) {

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            aboutService.changePassword(userId, request);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Password change validation failed for userId {0}: {1}",
                    new Object[]{userId, e.getMessage()});
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to change password for userId {0}: {1}",
                    new Object[]{userId, e.getMessage()});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to change password.");
        }
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            return principal.getUser().getId();
        } catch (ClassCastException e) {
            LOGGER.log(Level.WARNING, "Unexpected principal type in AboutController");
            return null;
        }
    }
}
