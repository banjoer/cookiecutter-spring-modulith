package {{ cookiecutter.package_name }}.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import {{ cookiecutter.package_name }}.identity.application.UserAccountService;
import {{ cookiecutter.package_name }}.identity.domain.UserAccountRepository;
import {{ cookiecutter.package_name }}.identity.infrastructure.security.JpaUserDetailsService;
import {{ cookiecutter.package_name }}.identity.web.IdentityController;

/**
 * Activation boundary and explicit wiring for the core identity module.
 *
 * <p>The module is registered by {@code ApplicationModulesConfiguration}. Its beans are only
 * registered when {@code app.modules.identity.enabled} is true. The component scan is scoped to
 * this module; the application entry point deliberately does not scan the root package.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.modules.identity",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(IdentityProperties.class)
@EnableJpaRepositories(basePackageClasses = UserAccountRepository.class)
@ComponentScan(basePackageClasses = {
        UserAccountService.class,
        JpaUserDetailsService.class,
        IdentityController.class
})
public class IdentityModuleConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
