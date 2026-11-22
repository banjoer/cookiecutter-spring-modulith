package com.example.springmodulithapplication.messaging;

import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Import;


import com.example.springmodulithapplication.messaging.rabbitmq.RabbitMQConfiguration;


import com.example.springmodulithapplication.messaging.websocket.WebSocketConfiguration;


/**
 * Composition root for messaging infrastructure.
 *
 * <p>RabbitMQ AMQP and STOMP are independent adapters. Disabling one never disables the other.
 * Both are infrastructure concerns and intentionally are not business modules.</p>
 */
@Configuration(proxyBeanMethods = false)

@Import({

        RabbitMQConfiguration.class,



        WebSocketConfiguration.class

})

public class MessagingConfiguration {
}
