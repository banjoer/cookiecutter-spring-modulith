package {{ cookiecutter.package_name }}.integration;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import {{ cookiecutter.package_name }}.identity.api.IdentityApi;
import {{ cookiecutter.package_name }}.identity.api.RegisterUserCommand;
import {{ cookiecutter.package_name }}.support.AbstractPostgresIntegrationTest;
import {{ cookiecutter.package_name }}.support.IntegrationTest;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=true",
        "app.modules.notifications.email.enabled=true",
        "app.modules.notifications.email.from=no-reply@example.com",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
@Import(EmailNotificationsIntegrationTest.MailConfiguration.class)
class EmailNotificationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JavaMailSender mailSender;

    @Test
    void sendsTheWelcomeEmailWhenTheOptionalAdapterIsEnabled() {
        String username = "mail-" + UUID.randomUUID().toString().substring(0, 8);
        identity.register(new RegisterUserCommand(
                username, username + "@example.com", "correct-horse-battery"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                verify(mailSender).send(any(SimpleMailMessage.class)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfiguration {

        @Bean
        @Primary
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
