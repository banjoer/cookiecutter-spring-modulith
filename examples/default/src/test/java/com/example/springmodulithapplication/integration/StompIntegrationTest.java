package com.example.springmodulithapplication.integration;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.ApplicationContext;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.messaging.websocket.controller.ChatInboundMessage;
import com.example.springmodulithapplication.messaging.websocket.controller.ChatOutboundMessage;

import com.example.springmodulithapplication.messaging.rabbitmq.producer.RabbitMQEventPublisher;

import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

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


    @Autowired
    private ApplicationContext context;


    @Test
    void authenticatesConnectAndCompletesTheChatFlow() throws Exception {

        assertThat(context.getBeanNamesForType(RabbitMQEventPublisher.class)).isEmpty();

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
