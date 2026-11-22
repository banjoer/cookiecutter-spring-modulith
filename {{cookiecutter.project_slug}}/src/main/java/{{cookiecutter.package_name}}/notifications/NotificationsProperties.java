package {{ cookiecutter.package_name }}.notifications;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the optional notifications module.
 */
@Validated
@ConfigurationProperties(prefix = "app.modules.notifications")
public class NotificationsProperties {

    /**
     * Whether the notifications module and its beans are active.
     */
    private boolean enabled = true;

    /**
     * Optional nested email integration. Email is disabled by default and requires a JavaMailSender,
     * normally configured through Spring Boot's standard {@code spring.mail.*} properties.
     */
    @Valid
    private Email email = new Email();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public static class Email {

        /**
         * Activates welcome-email delivery. When true, {@code spring.mail.host} must be configured.
         */
        private boolean enabled;

        /**
         * Sender address for notification emails. Required when email delivery is enabled.
         */
        @jakarta.validation.constraints.Email
        @Size(max = 320)
        private String from;

        /**
         * Prefix added to all email subjects.
         */
        @Size(max = 80)
        private String subjectPrefix = "[Notifications]";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getSubjectPrefix() {
            return subjectPrefix;
        }

        public void setSubjectPrefix(String subjectPrefix) {
            this.subjectPrefix = subjectPrefix;
        }

        @AssertTrue(message = "app.modules.notifications.email.from must be set when email delivery is enabled")
        public boolean isConfigurationValid() {
            return !enabled || (from != null && !from.isBlank());
        }
    }
}
