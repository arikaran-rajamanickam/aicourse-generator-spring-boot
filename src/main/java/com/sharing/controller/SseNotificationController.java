package com.sharing.controller;

import com.sharing.event.NotificationEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/notifications")
public class SseNotificationController {
    private static final Logger logger = Logger.getLogger(SseNotificationController.class.getName());

    // Map of userId to their active SSE emitter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@PathVariable Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Keep open indefinitely

        this.emitters.put(userId, emitter);

        emitter.onCompletion(() -> this.emitters.remove(userId));
        emitter.onTimeout(() -> this.emitters.remove(userId));
        emitter.onError((e) -> this.emitters.remove(userId));

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of("message", "Connected to real-time notification stream")));
        } catch (IOException e) {
            this.emitters.remove(userId);
        }

        return emitter;
    }

    public void sendNotification(Long userId, NotificationEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event));
                logger.info("Sent real-time notification to user " + userId + ": " + event.getType());
            } catch (IOException e) {
                emitters.remove(userId);
                logger.warning("Failed to send notification to user " + userId + ", removing emitter.");
            }
        }
    }
}
