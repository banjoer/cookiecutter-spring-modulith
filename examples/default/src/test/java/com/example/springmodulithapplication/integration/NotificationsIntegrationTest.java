package com.example.springmodulithapplication.integration;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.notifications.api.NotificationApi;
import com.example.springmodulithapplication.notifications.api.NotificationView;
import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=true",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
class NotificationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private NotificationApi notifications;

    @Test
    void createsANotificationFromAUserRegisteredEvent() {
        String username = "notify-" + UUID.randomUUID().toString().substring(0, 8);
        UserProfile user = identity.register(
                new RegisterUserCommand(username, username + "@example.com", "correct-horse-battery"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<NotificationView> views = notifications.findByRecipient(username);
            assertThat(views).hasSize(1);
            assertThat(views.getFirst().subject()).contains("Welcome");
        });

        NotificationView view = notifications.findByRecipient(username).getFirst();
        NotificationView read = notifications.markRead(view.id(), username);
        assertThat(read.read()).isTrue();
        assertThat(read.id()).isNotNull();
        assertThat(user.username()).isEqualTo(username);
    }
}
