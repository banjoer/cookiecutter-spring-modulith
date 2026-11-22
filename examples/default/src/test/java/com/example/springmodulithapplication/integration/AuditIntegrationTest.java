package com.example.springmodulithapplication.integration;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.springmodulithapplication.audit.api.AuditApi;
import com.example.springmodulithapplication.audit.api.AuditEntryView;
import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=true",
        "app.modules.audit.enabled=true",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
class AuditIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private AuditApi audit;

    @Test
    void recordsIdentityEventsAsynchronously() {
        String username = "audit-" + UUID.randomUUID().toString().substring(0, 8);
        UserProfile user = identity.register(
                new RegisterUserCommand(username, username + "@example.com", "correct-horse-battery"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(audit.recent(100))
                    .anySatisfy(entry -> {
                        assertThat(entry.eventType()).isEqualTo("identity.UserRegistered");
                        assertThat(entry.aggregateId()).isEqualTo(user.id());
                    });
        });

        AuditEntryView entry = audit.recent(100).stream()
                .filter(candidate -> candidate.aggregateId().equals(user.id()))
                .findFirst()
                .orElseThrow();
        assertThat(entry.actor()).isEqualTo(username);
        assertThat(entry.summary()).contains(username);
    }
}
