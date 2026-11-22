package {{ cookiecutter.package_name }}.notifications.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port owned by the notifications module.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    Optional<Notification> findByIdAndRecipientUsername(UUID id, String recipientUsername);
}
