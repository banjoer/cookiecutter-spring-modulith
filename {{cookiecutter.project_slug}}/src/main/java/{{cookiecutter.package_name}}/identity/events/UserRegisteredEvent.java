package {{ cookiecutter.package_name }}.identity.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a user has been persisted successfully.
 *
 * <p>The event is deliberately part of the public {@code identity :: events} named interface so
 * other modules can react without accessing identity internals.</p>
 */
public record UserRegisteredEvent(
        UUID userId,
        String username,
        String email,
        Instant registeredAt) {
}
