package com.aicourse.utils.exception;

import com.aicourse.utils.api.ApiResponse;
import com.features.FeatureAccessDeniedException;
import com.sharing.exception.SharedCourseContentLockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        LOGGER.log(Level.WARNING, "Bad Request: {0}", new Object[]{ex.getMessage()});
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        LOGGER.log(Level.SEVERE, "Unhandled exception: {0}", new Object[]{ex.getMessage()});
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal server error"));
    }

    @ExceptionHandler(FeatureAccessDeniedException.class)
    public ResponseEntity<String> handleFeatureAccessDenied(FeatureAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(SharedCourseContentLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSharedCourseContentLocked(SharedCourseContentLockedException ex) {
        LOGGER.log(Level.INFO, "Content access locked: {0}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage()));
    }
}