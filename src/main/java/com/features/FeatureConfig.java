package com.features;

/**
 * Holds the access config for one feature for one role.
 * limit = -1 means unlimited.
 */
public class FeatureConfig {

    public static final FeatureConfig DENIED = new FeatureConfig(false, 0);
    public static final FeatureConfig UNLIMITED = new FeatureConfig(true, -1);
    private final boolean allowed;
    private final int limit; // -1 = unlimited

    private FeatureConfig(boolean allowed, int limit) {
        this.allowed = allowed;
        this.limit = limit;
    }

    public static FeatureConfig withLimit(int limit) {
        return new FeatureConfig(true, limit);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isUnlimited() {
        return limit == -1;
    }
}