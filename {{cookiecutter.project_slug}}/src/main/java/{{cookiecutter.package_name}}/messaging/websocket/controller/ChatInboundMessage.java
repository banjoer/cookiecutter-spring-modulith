package {{ cookiecutter.package_name }}.messaging.websocket.controller;

/**
 * Client-to-server STOMP payload.
 */
public record ChatInboundMessage(String content) {
}
