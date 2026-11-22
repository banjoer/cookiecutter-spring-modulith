{% set rabbit = cookiecutter.include_rabbitmq|string|lower == 'true' -%}
{% set stomp = cookiecutter.include_stomp|string|lower == 'true' -%}
package {{ cookiecutter.package_name }}.messaging;

import org.springframework.context.annotation.Configuration;
{% if rabbit or stomp %}
import org.springframework.context.annotation.Import;
{% endif %}
{% if rabbit %}
import {{ cookiecutter.package_name }}.messaging.rabbitmq.RabbitMQConfiguration;
{% endif %}
{% if stomp %}
import {{ cookiecutter.package_name }}.messaging.websocket.WebSocketConfiguration;
{% endif %}

/**
 * Composition root for messaging infrastructure.
 *
 * <p>RabbitMQ AMQP and STOMP are independent adapters. Disabling one never disables the other.
 * Both are infrastructure concerns and intentionally are not business modules.</p>
 */
@Configuration(proxyBeanMethods = false)
{% if rabbit or stomp %}
@Import({
{% if rabbit %}
        RabbitMQConfiguration.class{% if stomp %},{% endif %}

{% endif %}
{% if stomp %}
        WebSocketConfiguration.class
{% endif %}
})
{% endif %}
public class MessagingConfiguration {
}
