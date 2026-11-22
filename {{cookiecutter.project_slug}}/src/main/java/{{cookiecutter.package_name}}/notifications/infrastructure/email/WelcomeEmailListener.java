package {{ cookiecutter.package_name }}.notifications.infrastructure.email;

import org.springframework.modulith.events.ApplicationModuleListener;

import {{ cookiecutter.package_name }}.identity.events.UserRegisteredEvent;

/**
 * Sends the optional welcome email after the user registration transaction commits.
 */
public class WelcomeEmailListener {

    private final EmailNotificationSender sender;

    public WelcomeEmailListener(EmailNotificationSender sender) {
        this.sender = sender;
    }

    @ApplicationModuleListener
    void onUserRegistered(UserRegisteredEvent event) {
        sender.sendWelcome(event);
    }
}
