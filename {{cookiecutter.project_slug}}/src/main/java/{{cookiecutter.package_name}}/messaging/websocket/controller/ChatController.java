package {{ cookiecutter.package_name }}.messaging.websocket.controller;

import java.time.Instant;
import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import {{ cookiecutter.package_name }}.messaging.websocket.WebSocketProperties;

/**
 * Minimal request/reply STOMP flow.
 *
 * <p>Send to {@code /app/chat.send}; the response is broadcast on the configured broker prefix,
 * by default {@code /topic/chat}. The destination prefixes are configurable through
 * {@code app.integrations.stomp.*}.</p>
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties properties;

    public ChatController(SimpMessagingTemplate messagingTemplate, WebSocketProperties properties) {
        this.messagingTemplate = messagingTemplate;
        this.properties = properties;
    }

    @MessageMapping("/chat.send")
    public void send(ChatInboundMessage message, Principal principal) {
        String content = message.content() == null ? "" : message.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Chat content must not be blank.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Chat content must not exceed 1000 characters.");
        }
        String author = principal == null ? "anonymous" : principal.getName();
        messagingTemplate.convertAndSend(
                properties.getBrokerPrefix() + "/chat",
                new ChatOutboundMessage(author, content, Instant.now()));
    }
}
