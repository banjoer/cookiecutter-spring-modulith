package {{ cookiecutter.package_name }}.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import {{ cookiecutter.package_name }}.identity.IdentityModuleConfiguration;
import {{ cookiecutter.package_name }}.infrastructure.InfrastructureConfiguration;
import {{ cookiecutter.package_name }}.messaging.MessagingConfiguration;

/**
 * Explicit registration of the core modules and the infrastructure composition root.
 *
 * <p>Only genuinely core modules belong here. Optional modules (notifications and audit) register
 * themselves through Spring Boot auto-configuration so that this class does not need to change
 * when an optional capability is toggled.</p>
 */
@Configuration(proxyBeanMethods = false)
@Import({
        IdentityModuleConfiguration.class,
        InfrastructureConfiguration.class,
        MessagingConfiguration.class
})
public class ApplicationModulesConfiguration {
}
