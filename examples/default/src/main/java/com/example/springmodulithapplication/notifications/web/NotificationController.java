package com.example.springmodulithapplication.notifications.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springmodulithapplication.notifications.api.NotificationApi;
import com.example.springmodulithapplication.notifications.api.NotificationView;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationApi notifications;

    public NotificationController(NotificationApi notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationView> list(Authentication authentication) {
        return notifications.findByRecipient(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public NotificationView markRead(@PathVariable UUID id, Authentication authentication) {
        return notifications.markRead(id, authentication.getName());
    }
}
