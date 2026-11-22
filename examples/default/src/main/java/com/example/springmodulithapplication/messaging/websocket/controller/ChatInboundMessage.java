package com.example.springmodulithapplication.messaging.websocket.controller;

/**
 * Client-to-server STOMP payload.
 */
public record ChatInboundMessage(String content) {
}
