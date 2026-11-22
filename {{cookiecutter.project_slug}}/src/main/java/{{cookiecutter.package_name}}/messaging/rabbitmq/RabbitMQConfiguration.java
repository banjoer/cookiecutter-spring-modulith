package {{ cookiecutter.package_name }}.messaging.rabbitmq;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import {{ cookiecutter.package_name }}.messaging.rabbitmq.consumer.RabbitMQEventConsumer;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.DomainEventOutboxListener;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.OutboxMessageRepository;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.outbox.OutboxRelay;
import {{ cookiecutter.package_name }}.messaging.rabbitmq.producer.RabbitMQEventPublisher;

import tools.jackson.databind.json.JsonMapper;

/**
 * Activation boundary and topology for the RabbitMQ AMQP adapter.
 *
 * <p>AMQP is independent from STOMP. Enabling this class has no effect on the STOMP relay, and
 * disabling STOMP has no effect on AMQP.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.integrations.rabbitmq",
        name = "enabled",
        havingValue = "true")
@EnableConfigurationProperties(RabbitMQProperties.class)
@EnableJpaRepositories(basePackageClasses = OutboxMessageRepository.class)
@EnableScheduling
@ComponentScan(basePackageClasses = {
        RabbitMQEventPublisher.class,
        RabbitMQEventConsumer.class,
        OutboxRelay.class,
        DomainEventOutboxListener.class
})
public class RabbitMQConfiguration {

    @Bean
    MessageConverter rabbitMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    TopicExchange applicationEventsExchange(RabbitMQProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    DirectExchange applicationDeadLetterExchange(RabbitMQProperties properties) {
        return new DirectExchange(properties.getExchange() + ".dlx", true, false);
    }

    @Bean
    Queue applicationEventsQueue(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueuePrefix() + ".events")
                .withArguments(Map.of(
                        "x-dead-letter-exchange", properties.getExchange() + ".dlx",
                        "x-dead-letter-routing-key", "dead-letter"))
                .build();
    }

    @Bean
    Queue applicationDeadLetterQueue(RabbitMQProperties properties) {
        return QueueBuilder.durable(properties.getQueuePrefix() + ".events.dlq").build();
    }

    @Bean
    Binding applicationEventsBinding(
            Queue applicationEventsQueue,
            TopicExchange applicationEventsExchange,
            RabbitMQProperties properties) {
        return BindingBuilder.bind(applicationEventsQueue)
                .to(applicationEventsExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    Binding applicationEventsDeadLetterBinding(
            Queue applicationDeadLetterQueue,
            DirectExchange applicationDeadLetterExchange) {
        return BindingBuilder.bind(applicationDeadLetterQueue)
                .to(applicationDeadLetterExchange)
                .with("dead-letter");
    }

    /**
     * Fails fast when the adapter is enabled but the broker or the reliability settings required by
     * the outbox are missing. Runtime unavailability after startup is retried by the outbox relay.
     */
    @Bean
    ApplicationRunner rabbitMQStartupVerifier(
            ConnectionFactory connectionFactory,
            Environment environment) {
        return args -> {
            String confirmType = environment.getProperty("spring.rabbitmq.publisher-confirm-type");
            if (!"correlated".equalsIgnoreCase(confirmType)) {
                throw new IllegalStateException(
                        "RabbitMQ integration is enabled but publisher confirms are not configured. "
                                + "Set spring.rabbitmq.publisher-confirm-type=correlated.");
            }
            if (!environment.getProperty("spring.rabbitmq.publisher-returns", Boolean.class, false)) {
                throw new IllegalStateException(
                        "RabbitMQ integration is enabled but publisher returns are disabled. "
                                + "Set spring.rabbitmq.publisher-returns=true.");
            }
            if (!environment.getProperty("spring.rabbitmq.template.mandatory", Boolean.class, false)) {
                throw new IllegalStateException(
                        "RabbitMQ integration is enabled but mandatory publishing is disabled. "
                                + "Set spring.rabbitmq.template.mandatory=true.");
            }
            try (var ignored = connectionFactory.createConnection()) {
                // A successful connection is enough to prove the broker is reachable at startup.
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "RabbitMQ integration is enabled, but the broker is not reachable. "
                                + "Start the compose service or set spring.rabbitmq.* connection properties.",
                        exception);
            }
        };
    }
}
