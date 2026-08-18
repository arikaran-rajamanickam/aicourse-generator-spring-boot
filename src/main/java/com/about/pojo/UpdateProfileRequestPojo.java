package com.about.pojo;

public class UpdateProfileRequestPojo {
    private String newUsername;
    private String displayName;
    private String handle;

    public UpdateProfileRequestPojo() {
    }

    public UpdateProfileRequestPojo(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }
}
