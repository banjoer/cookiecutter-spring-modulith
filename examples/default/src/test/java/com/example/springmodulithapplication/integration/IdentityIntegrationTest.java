package com.example.springmodulithapplication.integration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.shared.error.ConflictException;
import com.example.springmodulithapplication.support.AbstractPostgresIntegrationTest;
import com.example.springmodulithapplication.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@SpringBootTest(properties = {
        "app.modules.identity.enabled=true",
        "app.modules.notifications.enabled=false",
        "app.modules.audit.enabled=false",
        "app.integrations.rabbitmq.enabled=false",
        "app.integrations.stomp.enabled=false"
})
class IdentityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdentityApi identity;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsAreApplied() {
        Integer applied = jdbcTemplate.queryForObject("select count(*) from flyway_schema_history", Integer.class);
        assertThat(applied).isPositive();
        Integer users = jdbcTemplate.queryForObject("select count(*) from identity_user_accounts", Integer.class);
        assertThat(users).isNotNull();
    }

    @Test
    void registersAUserAndRejectsADuplicateUsername() {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.com";

        UserProfile profile = identity.register(new RegisterUserCommand(username, email, "correct-horse-battery"));

        assertThat(profile.id()).isNotNull();
        assertThat(profile.username()).isEqualTo(username);
        assertThat(profile.email()).isEqualTo(email);
        assertThat(identity.findByUsername(username)).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(profile.id());
            assertThat(found.username()).isEqualTo(profile.username());
            assertThat(found.email()).isEqualTo(profile.email());
        });

        assertThatThrownBy(() -> identity.register(
                new RegisterUserCommand(username, "other-" + email, "correct-horse-battery")))
                .isInstanceOf(ConflictException.class);
    }
}
