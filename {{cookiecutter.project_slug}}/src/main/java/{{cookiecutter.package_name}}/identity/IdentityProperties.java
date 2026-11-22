package {{ cookiecutter.package_name }}.identity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the identity module.
 */
@Validated
@ConfigurationProperties(prefix = "app.modules.identity")
public class IdentityProperties {

    /**
     * Whether the identity module and its beans are active.
     */
    private boolean enabled = true;

    /**
     * Whether public self-registration is permitted. Disable this when users are provisioned by
     * an external identity provider or an administrative workflow.
     */
    private boolean registrationEnabled = true;

    /**
     * Minimum accepted password length. Passwords are stored as BCrypt hashes.
     */
    @Min(8)
    @Max(128)
    private int minimumPasswordLength = 12;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public int getMinimumPasswordLength() {
        return minimumPasswordLength;
    }

    public void setMinimumPasswordLength(int minimumPasswordLength) {
        this.minimumPasswordLength = minimumPasswordLength;
    }
}
