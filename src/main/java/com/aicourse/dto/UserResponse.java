package com.aicourse.dto;

@Deprecated
public class UserResponse extends com.auth.dto.UserResponse {
    public UserResponse(Long id, String handle, String displayName, com.auth.enums.UserRole role) {
        super(id, handle, displayName, role);
    }

    public UserResponse(com.auth.model.Users user) {
        super(user);
    }
}
