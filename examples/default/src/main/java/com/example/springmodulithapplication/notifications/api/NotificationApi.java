package com.example.springmodulithapplication.notifications.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API of the notifications module.
 */
public interface NotificationApi {

    List<NotificationView> findByRecipient(String username);

    NotificationView markRead(UUID notificationId, String username);
}
