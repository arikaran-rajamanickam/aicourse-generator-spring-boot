package com.sharing.service.impl;

import com.sharing.controller.SseNotificationController;
import com.sharing.event.NotificationEvent;
import com.sharing.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private SseNotificationController sseController;

    @Override
    public void sendNotification(Long userId, NotificationEvent event) {
        sseController.sendNotification(userId, event);
    }
}
