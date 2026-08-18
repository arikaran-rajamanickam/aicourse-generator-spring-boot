package com.auth.dto;

import com.auth.enums.UserRole;
import com.auth.model.Users;

public class UserResponse {

    private final Long id;
    private final String handle;
    private final String displayName;
    private final UserRole role;

    public UserResponse(Long id, String handle, String displayName, UserRole role) {
        this.id = id;
        this.handle = handle;
        this.displayName = displayName;
        this.role = role;
    }

    public UserResponse(Users user) {
        this.id = user.getId();
        this.handle = user.getUsername();
        this.displayName = (user.getDisplayName() == null || user.getDisplayName().isBlank())
                ? user.getUsername()
                : user.getDisplayName();
        this.role = user.getRoles();
    }

    public Long getId() {
        return id;
    }

    public String getHandle() {
        return handle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUsername() {
        return handle;
    }

    public UserRole getRole() {
        return role;
    }
}
