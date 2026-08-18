package com.features;

public class FeatureAccessDeniedException extends RuntimeException {
    public FeatureAccessDeniedException(String message) {
        super(message);
    }
}