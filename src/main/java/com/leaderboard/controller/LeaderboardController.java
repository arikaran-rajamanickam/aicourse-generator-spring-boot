package com.leaderboard.controller;

import com.auth.model.UserPrincipal;
import com.leaderboard.dto.PagedLeaderboardDTO;
import com.leaderboard.dto.UserRankDTO;
import com.leaderboard.service.impl.GlobalLeaderboardService;
import com.leaderboard.service.impl.WeeklyLeaderboardService;
import com.leaderboard.weeklyupdate.WeeklyLeaderboardResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private static final Logger LOGGER = Logger.getLogger(LeaderboardController.class.getName());

    @Autowired
    private GlobalLeaderboardService globalLeaderboardService;

    @Autowired
    private WeeklyLeaderboardService weeklyLeaderboardService;

    @Autowired
    private WeeklyLeaderboardResetService weeklyLeaderboardResetService;

    @GetMapping("/global")
    public ResponseEntity<?> getGlobalLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PagedLeaderboardDTO result = globalLeaderboardService.getTopGlobalUsers(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching global leaderboard: " + e.getMessage());
        }
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        throw new UnsupportedOperationException("LeaderboardController.getWeeklyLeaderboard is not implemented yet");
    }

    @PostMapping("/weekly/reset")
    public ResponseEntity<?> resetWeeklyLeaderboard(Authentication authentication) {
        throw new UnsupportedOperationException("LeaderboardController.resetWeeklyLeaderboard is not implemented yet");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyRank(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to /api/leaderboard/me");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Long userId = principal.getUser().getId();

            LOGGER.log(Level.INFO, "Fetching leaderboard rank for userId: {0}", new Object[]{userId});

            UserRankDTO rank = globalLeaderboardService.getUserRankDTO(userId);

            if (rank == null) {
                LOGGER.log(Level.WARNING, "No leaderboard entry found for userId: {0}", new Object[]{userId});
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found in leaderboard. Complete some lessons to appear!");
            }
            return ResponseEntity.ok(rank);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching rank for current user: {0}", new Object[]{e.getMessage()});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch your rank. Please try again later.");
        }
    }
}