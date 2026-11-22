package com.example.springmodulithapplication.notifications;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.springmodulithapplication.notifications.application.NotificationService;
import com.example.springmodulithapplication.notifications.domain.NotificationRepository;
import com.example.springmodulithapplication.notifications.infrastructure.email.EmailNotificationSender;
import com.example.springmodulithapplication.notifications.infrastructure.email.WelcomeEmailListener;
import com.example.springmodulithapplication.notifications.web.NotificationController;

/**
 * Spring Boot auto-configuration for the optional notifications module.
 *
 * <p>This is the activation boundary for the module. When the property is false, none of the
 * module's controllers, services, listeners, repositories, or email adapters are registered. The
 * source remains compiled so the capability can be toggled without rebuilding a different artifact.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "app.modules.notifications",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(NotificationsProperties.class)
@EnableJpaRepositories(basePackageClasses = NotificationRepository.class)
@ComponentScan(basePackageClasses = {NotificationService.class, NotificationController.class})
public class NotificationsAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "app.modules.notifications.email",
            name = "enabled",
            havingValue = "true")
    static class EmailConfiguration {

        @Bean
        EmailNotificationSender emailNotificationSender(
                ObjectProvider<JavaMailSender> mailSenderProvider,
                NotificationsProperties properties) {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                throw new IllegalStateException(
                        "Email notifications are enabled, but no JavaMailSender is available. "
                                + "Configure spring.mail.host and the related spring.mail.* properties.");
            }
            return new EmailNotificationSender(mailSender, properties);
        }

        @Bean
        WelcomeEmailListener welcomeEmailListener(EmailNotificationSender sender) {
            return new WelcomeEmailListener(sender);
        }
    }
}
