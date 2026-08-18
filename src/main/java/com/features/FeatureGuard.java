package com.features;

import com.auth.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Injectable guard — call this in any service to enforce feature access.
 * <p>
 * Usage in a service:
 * featureGuard.requireAccess(Feature.COURSE_CREATE, userRole);
 * featureGuard.requireWithinLimit(Feature.COURSE_CREATE, userRole, currentCourseCount);
 */
@Component
public class FeatureGuard {

    @Autowired
    private FeatureRegistry featureRegistry;

    /**
     * Throws FeatureAccessDeniedException if the role does not have access.
     */
    public void requireAccess(Feature feature, UserRole role) {
        FeatureConfig config = featureRegistry.getConfig(feature, role);
        if (!config.isAllowed()) {
            throw new FeatureAccessDeniedException(
                    "Access denied: feature '" + feature.name() + "' is not available for your plan."
            );
        }
    }

    /**
     * Throws FeatureAccessDeniedException if the role is over its limit.
     * If limit is -1 (unlimited), always passes.
     */
    public void requireWithinLimit(Feature feature, UserRole role, int currentCount) {
        FeatureConfig config = featureRegistry.getConfig(feature, role);
        if (!config.isAllowed()) {
            throw new FeatureAccessDeniedException(
                    "Access denied: feature '" + feature.name() + "' is not available for your plan."
            );
        }
        if (!config.isUnlimited() && currentCount >= config.getLimit()) {
            throw new FeatureAccessDeniedException(
                    "Limit reached: you can only have " + config.getLimit()
                            + " " + feature.name().toLowerCase().replace("_", " ") + "(s) on your current plan."
            );
        }
    }
}