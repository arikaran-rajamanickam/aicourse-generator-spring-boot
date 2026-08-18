package com.about.service.impl;

import com.about.pojo.ChangePasswordRequestPojo;
import com.about.pojo.ProfileResponsePojo;
import com.about.pojo.UpdateProfileRequestPojo;
import com.about.service.AboutService;
import com.auth.model.Users;
import com.auth.repo.UserRepo;
import com.auth.service.UserService;
import com.leaderboard.model.UserStats;
import com.leaderboard.repository.UserStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Service
public class AboutServiceImpl implements AboutService {

    private static final Logger LOGGER = Logger.getLogger(AboutServiceImpl.class.getName());

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-z0-9._]{6,25}$");

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserStatsRepository userStatsRepository;

    @Override
    public ProfileResponsePojo getProfile(Long userId) throws Exception {
        LOGGER.log(Level.INFO, "Fetching profile for userId: {0}", userId);

        Users user = findUserOrThrow(userId);
        String displayName = (user.getDisplayName() == null || user.getDisplayName().isBlank())
                ? user.getUsername()
                : user.getDisplayName();

        // UserStats row might not exist yet if the user never completed a lesson
        ProfileResponsePojo.StatsSnapshot stats = userStatsRepository
                .findByUserId(userId)
                .map(this::toSnapshot)
                .orElse(emptySnapshot());

        return new ProfileResponsePojo(
                user.getId(),
                user.getUsername(),
                displayName,
                user.getRoles(),
                user.getCreatedAt(),
                stats
        );
    }

    @Override
    public ProfileResponsePojo updateProfile(Long userId, UpdateProfileRequestPojo request) throws Exception {
        LOGGER.log(Level.INFO, "Updating profile for userId: {0}", userId);

        String requestedDisplayName = request.getDisplayName();
        if ((requestedDisplayName == null || requestedDisplayName.isBlank()) && request.getNewUsername() != null) {
            requestedDisplayName = request.getNewUsername();
        }
        String requestedHandle = request.getHandle();

        Users user = findUserOrThrow(userId);

        if (requestedDisplayName != null) {
            String trimmed = requestedDisplayName.trim();
            if (trimmed.isBlank()) {
                throw new IllegalArgumentException("Display name cannot be blank.");
            }
            user.setDisplayName(trimmed);
        }

        if (requestedHandle != null) {
            String trimmedHandle = requestedHandle.trim();
            if (!HANDLE_PATTERN.matcher(trimmedHandle).matches()) {
                throw new IllegalArgumentException("User ID must be 6-25 characters and use only lowercase letters, numbers, '.' or '_' .");
            }

            if (!user.getUsername().equals(trimmedHandle)) {
                Users existing = userRepo.findByUsername(trimmedHandle);
                if (existing != null && !existing.getId().equals(userId)) {
                    throw new IllegalArgumentException("User ID '" + trimmedHandle + "' is already taken.");
                }
                user.setUsername(trimmedHandle);
            }
        }

        userRepo.save(user);

        LOGGER.log(Level.INFO, "Profile updated successfully for userId: {0}", userId);
        return getProfile(userId);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequestPojo request) throws Exception {
        LOGGER.log(Level.INFO, "Password change request for userId: {0}", userId);
        BCryptPasswordEncoder encoder = UserService.getEncoder();

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // --- Basic null / blank guards ---
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password cannot be blank.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be blank.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Confirm password cannot be blank.");
        }

        // --- New password length ---
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "New password must be at least " + MIN_PASSWORD_LENGTH + " characters long."
            );
        }

        // --- new == confirm ---
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }

        Users user = findUserOrThrow(userId);

        // --- Verify current password against stored BCrypt hash ---
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // --- Prevent using the same password ---
        if (encoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);

        LOGGER.log(Level.INFO, "Password changed successfully for userId: {0}", userId);
    }

    private Users findUserOrThrow(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> {
                    LOGGER.log(Level.SEVERE, "User not found with id: {0}", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });
    }

    private ProfileResponsePojo.StatsSnapshot toSnapshot(UserStats us) {
        return new ProfileResponsePojo.StatsSnapshot(
                us.getTotalPoints(),
                us.getWeeklyPoints(),
                us.getCoursesCompleted(),
                us.getLessonsCompleted(),
                us.getCurrentStreak()
        );
    }

    private ProfileResponsePojo.StatsSnapshot emptySnapshot() {
        return new ProfileResponsePojo.StatsSnapshot(0, 0, 0, 0, 0);
    }
}
