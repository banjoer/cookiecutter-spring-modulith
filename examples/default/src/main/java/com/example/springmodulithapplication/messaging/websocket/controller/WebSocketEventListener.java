package com.example.springmodulithapplication.messaging.websocket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Observability hook for WebSocket session lifecycle events.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.info("WebSocket session connected: {}", event.getUser() == null ? "anonymous" : event.getUser().getName());
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        log.info("WebSocket session disconnected: {}", event.getSessionId());
    }
}
