{% set rabbit = cookiecutter.include_rabbitmq|string|lower == 'true' -%}
package {{ cookiecutter.package_name }}.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import {{ cookiecutter.package_name }}.audit.api.AuditApi;
import {{ cookiecutter.package_name }}.audit.domain.AuditEntryRepository;
import {{ cookiecutter.package_name }}.identity.api.IdentityApi;
import {{ cookiecutter.package_name }}.identity.domain.UserAccountRepository;
{% if rabbit %}
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.OutboxMessageRepository;
{% endif %}
import {{ cookiecutter.package_name }}.notifications.api.NotificationApi;
import {{ cookiecutter.package_name }}.notifications.domain.NotificationRepository;
import {{ cookiecutter.package_name }}.support.AbstractPostgresIntegrationTest;
import {{ cookiecutter.package_name }}.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=false",
        "app.modules.notifications.enabled=false",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
class ActivationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void disabledModulesDoNotRegisterTheirBeans() {
        assertThat(context.getBeanNamesForType(IdentityApi.class)).isEmpty();
        assertThat(context.getBeanNamesForType(NotificationApi.class)).isEmpty();
        assertThat(context.getBeanNamesForType(AuditApi.class)).isEmpty();
        assertThat(context.getBeanNamesForType(UserAccountRepository.class)).isEmpty();
        assertThat(context.getBeanNamesForType(NotificationRepository.class)).isEmpty();
        assertThat(context.getBeanNamesForType(AuditEntryRepository.class)).isEmpty();
{% if rabbit %}
        assertThat(context.getBeanNamesForType(OutboxMessageRepository.class)).isEmpty();
{% endif %}
    }

    @Test
    void applicationStillStartsWithAllOptionalModulesDisabled() {
        assertThat(context.getEnvironment().getProperty("app.modules.identity.enabled", Boolean.class)).isFalse();
        assertThat(context.getBean("securityFilterChain")).isNotNull();
    }
}
