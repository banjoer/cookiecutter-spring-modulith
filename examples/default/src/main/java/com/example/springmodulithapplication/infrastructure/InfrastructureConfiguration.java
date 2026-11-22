package com.example.springmodulithapplication.infrastructure;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.example.springmodulithapplication.infrastructure.security.SecurityConfiguration;
import com.example.springmodulithapplication.infrastructure.web.CorrelationIdFilter;
import com.example.springmodulithapplication.infrastructure.web.GlobalExceptionHandler;

/**
 * Wiring for cross-cutting infrastructure. This is not a business module and owns no business
 * persistence. Module-specific adapters stay inside their own modules.
 */
@Configuration(proxyBeanMethods = false)
@Import(SecurityConfiguration.class)
@ComponentScan(basePackageClasses = {CorrelationIdFilter.class, GlobalExceptionHandler.class})
public class InfrastructureConfiguration {
}
