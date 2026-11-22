package {{ cookiecutter.package_name }}.infrastructure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import {{ cookiecutter.package_name }}.infrastructure.security.SecurityConfiguration;
import {{ cookiecutter.package_name }}.infrastructure.web.CorrelationIdFilter;
import {{ cookiecutter.package_name }}.infrastructure.web.GlobalExceptionHandler;

/**
 * Wiring for cross-cutting infrastructure. This is not a business module and owns no business
 * persistence. Module-specific adapters stay inside their own modules.
 */
@Configuration(proxyBeanMethods = false)
@Import(SecurityConfiguration.class)
@ComponentScan(basePackageClasses = {CorrelationIdFilter.class, GlobalExceptionHandler.class})
public class InfrastructureConfiguration {
}
