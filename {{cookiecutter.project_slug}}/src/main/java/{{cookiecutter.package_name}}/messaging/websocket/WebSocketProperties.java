package {{ cookiecutter.package_name }}.messaging.websocket;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * STOMP/WebSocket integration configuration.
 *
 * <p>STOMP is independent from RabbitMQ AMQP. A simple in-memory broker is used for local
 * development; a STOMP broker relay can target an external broker such as RabbitMQ with the
 * {@code rabbitmq_stomp} plugin enabled.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app.integrations.stomp")
public class WebSocketProperties {

    public enum BrokerType {
        SIMPLE,
        RELAY
    }

    /**
     * Whether the WebSocket endpoint and STOMP message broker are active.
     */
    private boolean enabled;

    /**
     * WebSocket handshake endpoint.
     */
    @NotBlank
    private String endpoint = "/ws";

    /**
     * Prefix for client-to-server application destinations.
     */
    @NotBlank
    private String applicationPrefix = "/app";

    /**
     * Prefix for broker destinations.
     */
    @NotBlank
    private String brokerPrefix = "/topic";

    /**
     * Prefix for user-specific destinations.
     */
    @NotBlank
    private String userDestinationPrefix = "/user";

    /**
     * SIMPLE uses Spring's in-memory broker; RELAY forwards to an external STOMP broker.
     */
    private BrokerType brokerType = BrokerType.SIMPLE;

    /**
     * Whether CONNECT and subsequent client frames require an authenticated principal.
     */
    private boolean requireAuthentication = true;

    /**
     * Allowed browser origins for the WebSocket handshake.
     */
    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:8080"));

    /**
     * External broker relay settings, used only when {@link #brokerType} is RELAY.
     */
    @Valid
    private Relay relay = new Relay();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApplicationPrefix() {
        return applicationPrefix;
    }

    public void setApplicationPrefix(String applicationPrefix) {
        this.applicationPrefix = applicationPrefix;
    }

    public String getBrokerPrefix() {
        return brokerPrefix;
    }

    public void setBrokerPrefix(String brokerPrefix) {
        this.brokerPrefix = brokerPrefix;
    }

    public String getUserDestinationPrefix() {
        return userDestinationPrefix;
    }

    public void setUserDestinationPrefix(String userDestinationPrefix) {
        this.userDestinationPrefix = userDestinationPrefix;
    }

    public BrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public Relay getRelay() {
        return relay;
    }

    public void setRelay(Relay relay) {
        this.relay = relay;
    }

    public static class Relay {

        private String host;

        @Min(1)
        @Max(65535)
        private int port = 61613;

        private String clientLogin;

        private String clientPasscode;

        private String systemLogin;

        private String systemPasscode;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getClientLogin() {
            return clientLogin;
        }

        public void setClientLogin(String clientLogin) {
            this.clientLogin = clientLogin;
        }

        public String getClientPasscode() {
            return clientPasscode;
        }

        public void setClientPasscode(String clientPasscode) {
            this.clientPasscode = clientPasscode;
        }

        public String getSystemLogin() {
            return systemLogin;
        }

        public void setSystemLogin(String systemLogin) {
            this.systemLogin = systemLogin;
        }

        public String getSystemPasscode() {
            return systemPasscode;
        }

        public void setSystemPasscode(String systemPasscode) {
            this.systemPasscode = systemPasscode;
        }
    }

    @AssertTrue(message = "STOMP relay requires host, client credentials, and system credentials")
    public boolean isRelayConfigurationValid() {
        if (brokerType != BrokerType.RELAY) {
            return true;
        }
        return relay != null
                && relay.getHost() != null && !relay.getHost().isBlank()
                && relay.getClientLogin() != null && !relay.getClientLogin().isBlank()
                && relay.getClientPasscode() != null && !relay.getClientPasscode().isBlank()
                && relay.getSystemLogin() != null && !relay.getSystemLogin().isBlank()
                && relay.getSystemPasscode() != null && !relay.getSystemPasscode().isBlank();
    }
}
