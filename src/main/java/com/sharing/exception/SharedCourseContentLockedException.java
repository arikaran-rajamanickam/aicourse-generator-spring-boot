package com.sharing.exception;

public class SharedCourseContentLockedException extends RuntimeException {

    public SharedCourseContentLockedException(String message) {
        super(message);
    }
}

