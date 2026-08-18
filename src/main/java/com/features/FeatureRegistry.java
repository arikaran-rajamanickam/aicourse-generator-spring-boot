package com.features;

import com.auth.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class FeatureRegistry {

    // Master map: Feature → (Role → Config)
    private final Map<Feature, Map<UserRole, FeatureConfig>> registry = new EnumMap<>(Feature.class);

    public FeatureRegistry() {
        build();
    }

    private void build() {

        // ── COURSE_CREATE ──────────────────────────────────
        register(Feature.COURSE_CREATE,
                Map.of(
                        UserRole.USER, FeatureConfig.withLimit(3),
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── COURSE_DELETE ──────────────────────────────────
        register(Feature.COURSE_DELETE,
                Map.of(
                        UserRole.USER, FeatureConfig.UNLIMITED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── COURSE_RENAME ──────────────────────────────────
        register(Feature.COURSE_RENAME,
                Map.of(
                        UserRole.USER, FeatureConfig.UNLIMITED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── PROJECT_CREATE ─────────────────────────────────
        register(Feature.PROJECT_CREATE,
                Map.of(
                        UserRole.USER, FeatureConfig.withLimit(2),
                        UserRole.PREMIUM_USER, FeatureConfig.withLimit(20),
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── LESSON_GENERATE ────────────────────────────────
        register(Feature.LESSON_GENERATE,
                Map.of(
                        UserRole.USER, FeatureConfig.UNLIMITED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── AI_COACH ───────────────────────────────────────
        register(Feature.AI_COACH,
                Map.of(
                        UserRole.USER, FeatureConfig.UNLIMITED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── ADVANCED_COURSE_SETTINGS (premium only) ────────
        register(Feature.ADVANCED_COURSE_SETTINGS,
                Map.of(
                        UserRole.USER, FeatureConfig.DENIED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── API_KEY_COURSE_GENERATION (premium only) ───────
        register(Feature.API_KEY_COURSE_GENERATION,
                Map.of(
                        UserRole.USER, FeatureConfig.DENIED,
                        UserRole.PREMIUM_USER, FeatureConfig.UNLIMITED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── ADMIN_PANEL (admin only) ───────────────────────
        register(Feature.ADMIN_PANEL,
                Map.of(
                        UserRole.USER, FeatureConfig.DENIED,
                        UserRole.PREMIUM_USER, FeatureConfig.DENIED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── MANAGE_USERS (admin only) ──────────────────────
        register(Feature.MANAGE_USERS,
                Map.of(
                        UserRole.USER, FeatureConfig.DENIED,
                        UserRole.PREMIUM_USER, FeatureConfig.DENIED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );

        // ── VIEW_ALL_COURSES (admin only) ──────────────────
        register(Feature.VIEW_ALL_COURSES,
                Map.of(
                        UserRole.USER, FeatureConfig.DENIED,
                        UserRole.PREMIUM_USER, FeatureConfig.DENIED,
                        UserRole.ADMIN, FeatureConfig.UNLIMITED
                )
        );
    }

    private void register(Feature feature, Map<UserRole, FeatureConfig> roleMap) {
        registry.put(feature, new EnumMap<>(roleMap));
    }

    /**
     * Get the FeatureConfig for a given feature and role.
     * Falls back to DENIED if not configured.
     */
    public FeatureConfig getConfig(Feature feature, UserRole role) {
        Map<UserRole, FeatureConfig> roleMap = registry.get(feature);
        if (roleMap == null) return FeatureConfig.DENIED;
        return roleMap.getOrDefault(role, FeatureConfig.DENIED);
    }

    /**
     * Check if a role has access to a feature.
     */
    public boolean isAllowed(Feature feature, UserRole role) {
        return getConfig(feature, role).isAllowed();
    }

    /**
     * Returns all features + their config for a given role.
     * Used by the /api/features/me endpoint.
     */
    public Map<Feature, FeatureConfig> getAllForRole(UserRole role) {
        Map<Feature, FeatureConfig> result = new EnumMap<>(Feature.class);
        for (Feature feature : Feature.values()) {
            result.put(feature, getConfig(feature, role));
        }
        return result;
    }
}