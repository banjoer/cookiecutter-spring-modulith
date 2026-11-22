package {{ cookiecutter.package_name }}.identity.domain;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    void createsAccountWithoutExposingThePasswordHashInThePublicViewLayerContract() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        UserAccount account = new UserAccount(
                id, "alice", "alice@example.com", "bcrypt-hash", UserRole.USER, createdAt);

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getUsername()).isEqualTo("alice");
        assertThat(account.getEmail()).isEqualTo("alice@example.com");
        assertThat(account.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(account.getRole()).isEqualTo(UserRole.USER);
        assertThat(account.getCreatedAt()).isEqualTo(createdAt);
    }
}
