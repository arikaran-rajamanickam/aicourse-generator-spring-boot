package com.features;

import com.auth.enums.UserRole;
import com.auth.model.UserPrincipal;
import com.leaderboard.model.impl.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/features")
public class FeatureController {

    @Autowired
    private FeatureRegistry featureRegistry;

    @Autowired
    private UserStatsService userStatsService;

    /**
     * Returns all feature flags and limits for the currently authenticated user.
     * Frontend calls this once after login and stores it in FeatureContext.
     * <p>
     * Response shape:
     * {
     * "COURSE_CREATE":  { "allowed": true,  "limit": 3, "usage": 1 },
     * "PROJECT_CREATE": { "allowed": true,  "limit": 2, "usage": 0 },
     * "ADMIN_PANEL":    { "allowed": false, "limit": 0, "usage": 0 },
     * ...
     * }
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyFeatures(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserRole role = principal.getUser().getRoles();
        Long userId = principal.getUser().getId();

        Map<Feature, FeatureConfig> configs = featureRegistry.getAllForRole(role);

        // Convert to a clean JSON-friendly map
        Map<String, Map<String, Object>> response = new HashMap<>();
        for (Map.Entry<Feature, FeatureConfig> entry : configs.entrySet()) {
            Map<String, Object> configMap = new HashMap<>();
            configMap.put("allowed", entry.getValue().isAllowed());
            configMap.put("limit", entry.getValue().getLimit()); // -1 = unlimited

            int usage = 0;
            if (entry.getKey() == Feature.COURSE_CREATE) {
                usage = userStatsService.getTotalCoursesCreated(userId);
            } else if (entry.getKey() == Feature.PROJECT_CREATE) {
                usage = userStatsService.getTotalProjectsCreated(userId);
            }
            configMap.put("usage", usage);

            response.put(entry.getKey().name(), configMap);
        }

        return ResponseEntity.ok(response);
    }
}