package {{ cookiecutter.package_name }}.messaging.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import {{ cookiecutter.package_name }}.messaging.websocket.auth.StompAuthenticationInterceptor;
import {{ cookiecutter.package_name }}.messaging.websocket.controller.ChatController;
import {{ cookiecutter.package_name }}.messaging.websocket.controller.WebSocketEventListener;

/**
 * Conditional STOMP configuration with a configurable broker strategy.
 *
 * <p>When {@code broker-type=simple}, Spring's in-memory broker is used. When
 * {@code broker-type=relay}, the server connects to an external STOMP broker (for example
 * RabbitMQ with {@code rabbitmq_stomp} enabled). RabbitMQ AMQP remains independently controlled by
 * {@code app.integrations.rabbitmq.enabled}.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.integrations.stomp",
        name = "enabled",
        havingValue = "true")
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketProperties.class)
@ComponentScan(basePackageClasses = {ChatController.class, WebSocketEventListener.class})
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final ObjectProvider<AuthenticationManager> authenticationManager;

    public WebSocketConfiguration(
            WebSocketProperties properties,
            ObjectProvider<AuthenticationManager> authenticationManager) {
        this.properties = properties;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(properties.getApplicationPrefix());
        registry.setUserDestinationPrefix(properties.getUserDestinationPrefix());

        if (properties.getBrokerType() == WebSocketProperties.BrokerType.RELAY) {
            WebSocketProperties.Relay relay = properties.getRelay();
            registry.enableStompBrokerRelay(properties.getBrokerPrefix())
                    .setRelayHost(relay.getHost())
                    .setRelayPort(relay.getPort())
                    .setClientLogin(relay.getClientLogin())
                    .setClientPasscode(relay.getClientPasscode())
                    .setSystemLogin(relay.getSystemLogin())
                    .setSystemPasscode(relay.getSystemPasscode());
        } else {
            registry.enableSimpleBroker(properties.getBrokerPrefix());
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAuthenticationInterceptor(properties, authenticationManager));
    }

    /**
     * Stop a misconfigured relay deployment immediately instead of letting clients discover a dead
     * broker later. The port is closed when the RabbitMQ STOMP plugin is not enabled.
     */
    @Bean
    ApplicationRunner stompRelayStartupVerifier(WebSocketProperties properties) {
        return args -> {
            if (properties.getBrokerType() != WebSocketProperties.BrokerType.RELAY) {
                return;
            }
            WebSocketProperties.Relay relay = properties.getRelay();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(relay.getHost(), relay.getPort()), 3_000);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "STOMP relay is enabled, but " + relay.getHost() + ":" + relay.getPort()
                                + " is not reachable. Ensure the external broker is running and its STOMP "
                                + "plugin is enabled (for RabbitMQ: rabbitmq-plugins enable rabbitmq_stomp).",
                        exception);
            }
        };
    }
}
