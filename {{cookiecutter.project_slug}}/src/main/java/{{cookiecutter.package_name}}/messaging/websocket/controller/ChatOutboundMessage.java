package {{ cookiecutter.package_name }}.messaging.websocket.controller;

import java.time.Instant;

/**
 * Server-to-broker STOMP payload.
 */
public record ChatOutboundMessage(
        String author,
        String content,
        Instant sentAt) {
}
