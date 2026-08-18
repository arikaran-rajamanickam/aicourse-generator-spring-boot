package com.sharing.event;

public class NotificationEvent {
    private String type;
    private Long userId;
    private String message;
    private Object payload;

    public NotificationEvent() {
    }

    public NotificationEvent(String type, Long userId, String message, Object payload) {
        this.type = type;
        this.userId = userId;
        this.message = message;
        this.payload = payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public static class Builder {
        private final NotificationEvent event = new NotificationEvent();

        public Builder type(String type) {
            event.type = type;
            return this;
        }

        public Builder userId(Long userId) {
            event.userId = userId;
            return this;
        }

        public Builder message(String message) {
            event.message = message;
            return this;
        }

        public Builder payload(Object payload) {
            event.payload = payload;
            return this;
        }

        public NotificationEvent build() {
            return event;
        }
    }
}
