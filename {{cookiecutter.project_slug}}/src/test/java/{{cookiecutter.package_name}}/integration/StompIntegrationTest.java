{% set rabbit = cookiecutter.include_rabbitmq|string|lower == 'true' -%}
package {{ cookiecutter.package_name }}.integration;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
{% if rabbit %}
import org.springframework.context.ApplicationContext;
{% endif %}
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import {{ cookiecutter.package_name }}.identity.api.IdentityApi;
import {{ cookiecutter.package_name }}.identity.api.RegisterUserCommand;
import {{ cookiecutter.package_name }}.identity.api.UserProfile;
import {{ cookiecutter.package_name }}.messaging.websocket.controller.ChatInboundMessage;
import {{ cookiecutter.package_name }}.messaging.websocket.controller.ChatOutboundMessage;
{% if rabbit %}
import {{ cookiecutter.package_name }}.messaging.rabbitmq.producer.RabbitMQEventPublisher;
{% endif %}
import {{ cookiecutter.package_name }}.support.AbstractPostgresIntegrationTest;
import {{ cookiecutter.package_name }}.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.modules.identity.enabled=true",
                "app.modules.notifications.enabled=false",
                "app.modules.audit.enabled=false",
                "app.integrations.rabbitmq.enabled=false",
                "app.integrations.stomp.enabled=true",
                "app.integrations.stomp.require-authentication=true"
        })
class StompIntegrationTest extends AbstractPostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private IdentityApi identity;
{% if rabbit %}

    @Autowired
    private ApplicationContext context;
{% endif %}

    @Test
    void authenticatesConnectAndCompletesTheChatFlow() throws Exception {
{% if rabbit %}
        assertThat(context.getBeanNamesForType(RabbitMQEventPublisher.class)).isEmpty();
{% endif %}
        String username = "stomp-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "correct-horse-battery";
        UserProfile user = identity.register(
                new RegisterUserCommand(username, username + "@example.com", password));

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = null;
        try {
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8)));

            session = client.connectAsync(
                            "ws://localhost:" + port + "/ws",
                            new org.springframework.web.socket.WebSocketHttpHeaders(),
                            connectHeaders,
                            new StompSessionHandlerAdapter() {
                            })
                    .get(10, TimeUnit.SECONDS);

            CompletableFuture<ChatOutboundMessage> received = new CompletableFuture<>();
            session.subscribe("/topic/chat", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return ChatOutboundMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.complete((ChatOutboundMessage) payload);
                }
            });

            session.send("/app/chat.send", new ChatInboundMessage("hello modular monolith"));

            ChatOutboundMessage message = received.get(10, TimeUnit.SECONDS);
            assertThat(message.content()).isEqualTo("hello modular monolith");
            assertThat(message.author()).isEqualTo(user.username());
        } finally {
            if (session != null) {
                session.disconnect();
            }
            client.stop();
        }
    }
}
