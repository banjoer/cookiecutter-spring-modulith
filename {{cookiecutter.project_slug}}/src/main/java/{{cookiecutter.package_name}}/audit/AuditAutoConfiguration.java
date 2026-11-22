package {{ cookiecutter.package_name }}.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import {{ cookiecutter.package_name }}.audit.application.AuditService;
import {{ cookiecutter.package_name }}.audit.domain.AuditEntryRepository;
import {{ cookiecutter.package_name }}.audit.web.AuditController;

/**
 * Spring Boot auto-configuration for the optional audit module.
 *
 * <p>The module has no hard dependency on a broker or a scheduler. It consumes application events
 * asynchronously through the Spring Modulith event publication registry, so failed audit work is
 * visible and retryable rather than lost.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "app.modules.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
@EnableJpaRepositories(basePackageClasses = AuditEntryRepository.class)
@ComponentScan(basePackageClasses = {AuditService.class, AuditController.class})
public class AuditAutoConfiguration {
}
