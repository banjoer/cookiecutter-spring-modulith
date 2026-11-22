{% set stomp = cookiecutter.include_stomp|string|lower == 'true' -%}
package {{ cookiecutter.package_name }}.messaging.rabbitmq;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import {{ cookiecutter.package_name }}.identity.api.IdentityApi;
import {{ cookiecutter.package_name }}.identity.api.RegisterUserCommand;
import {{ cookiecutter.package_name }}.identity.api.UserProfile;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.InboxMessage;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.producer.RabbitMQEventPublisher;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.InboxMessageRepository;
{% if stomp %}
import {{ cookiecutter.package_name }}.messaging.websocket.controller.ChatController;
{% endif %}
import {{ cookiecutter.package_name }}.support.AbstractMessagingIntegrationTest;
import {{ cookiecutter.package_name }}.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=true",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=true",
        "app.integrations.stomp.enabled=false",
        "app.integrations.rabbitmq.outbox-poll-interval-ms=200"
})
class RabbitMQIntegrationTest extends AbstractMessagingIntegrationTest {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private InboxMessageRepository inbox;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext context;

    @Test
    void publishesDomainEventsThroughTheOutboxAndConsumesThemIdempotently() {
        String username = "rabbit-" + UUID.randomUUID().toString().substring(0, 8);
        UserProfile user = identity.register(
                new RegisterUserCommand(username, username + "@example.com", "correct-horse-battery"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(inbox.findByType("identity.UserRegistered"))
                    .anySatisfy(message -> assertThat(message.getAggregateId()).isEqualTo(user.id()));
        });

        InboxMessage message = inbox.findByType("identity.UserRegistered").stream()
                .filter(candidate -> candidate.getAggregateId().equals(user.id()))
                .findFirst()
                .orElseThrow();
        assertThat(message.getCorrelationId()).isNotBlank();

        Integer published = jdbcTemplate.queryForObject(
                "select count(*) from messaging_outbox where aggregate_id = ? and published_at is not null",
                Integer.class,
                user.id());
        assertThat(published).isEqualTo(1);
    }

    @Test
    void stompStaysDisabledWithoutAffectingAmqp() {
{% if stomp %}
        assertThat(context.getBeanNamesForType(ChatController.class)).isEmpty();
{% endif %}
        assertThat(context.getBeansOfType(RabbitMQEventPublisher.class)).isNotEmpty();
    }
}
