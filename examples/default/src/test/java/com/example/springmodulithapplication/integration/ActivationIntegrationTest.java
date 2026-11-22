package com.example.springmodulithapplication.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.example.springmodulithapplication.audit.api.AuditApi;
import com.example.springmodulithapplication.audit.domain.AuditEntryRepository;
import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.domain.UserAccountRepository;

import com.example.springmodulithapplication.messaging.rabbitmq.outbox.OutboxMessageRepository;

import com.example.springmodulithapplication.notifications.api.NotificationApi;
import com.example.springmodulithapplication.notifications.domain.NotificationRepository;
import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

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

        assertThat(context.getBeanNamesForType(OutboxMessageRepository.class)).isEmpty();

    }

    @Test
    void applicationStillStartsWithAllOptionalModulesDisabled() {
        assertThat(context.getEnvironment().getProperty("app.modules.identity.enabled", Boolean.class)).isFalse();
        assertThat(context.getBean("securityFilterChain")).isNotNull();
    }
}
