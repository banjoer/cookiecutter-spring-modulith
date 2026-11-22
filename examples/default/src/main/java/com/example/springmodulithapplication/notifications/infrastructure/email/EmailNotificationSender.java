package com.example.springmodulithapplication.notifications.infrastructure.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.example.springmodulithapplication.identity.events.UserRegisteredEvent;
import com.example.springmodulithapplication.notifications.NotificationsProperties;

/**
 * Optional email adapter. It is only instantiated when
 * {@code app.modules.notifications.email.enabled=true}.
 */
public class EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationsProperties properties;

    public EmailNotificationSender(JavaMailSender mailSender, NotificationsProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendWelcome(UserRegisteredEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFrom());
        message.setTo(event.email());
        message.setSubject(properties.getEmail().getSubjectPrefix() + " Welcome");
        message.setText("Hello " + event.username() + ",\n\nYour account is ready.\n");
        mailSender.send(message);
    }
}
