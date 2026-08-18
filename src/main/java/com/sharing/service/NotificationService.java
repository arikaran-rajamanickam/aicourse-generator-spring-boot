package com.sharing.service;

import com.sharing.event.NotificationEvent;

public interface NotificationService {
    void sendNotification(Long userId, NotificationEvent event);
}
