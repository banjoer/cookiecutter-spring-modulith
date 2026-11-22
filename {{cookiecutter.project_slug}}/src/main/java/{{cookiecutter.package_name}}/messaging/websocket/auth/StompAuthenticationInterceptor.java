package {{ cookiecutter.package_name }}.messaging.websocket.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import {{ cookiecutter.package_name }}.messaging.websocket.WebSocketProperties;

/**
 * Authentication integration point for STOMP.
 *
 * <p>For this starter, CONNECT carries HTTP Basic credentials in the {@code Authorization} native
 * header and is authenticated with the same {@link AuthenticationManager} as the REST API. Replace
 * this with your OAuth2/JWT or session strategy when integrating an external identity provider.</p>
 */
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final WebSocketProperties properties;
    private final ObjectProvider<AuthenticationManager> authenticationManager;

    public StompAuthenticationInterceptor(
            WebSocketProperties properties,
            ObjectProvider<AuthenticationManager> authenticationManager) {
        this.properties = properties;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand()) && properties.isRequireAuthentication()) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        if (properties.isRequireAuthentication()
                && (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))
                && accessor.getUser() == null) {
            throw new MessagingException("An authenticated STOMP session is required.");
        }
        return message;
    }

    private Authentication authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new MessagingException(
                    "STOMP CONNECT requires an HTTP Basic Authorization native header.");
        }

        try {
            String decoded = new String(
                    Base64.getDecoder().decode(authorization.substring("Basic ".length())),
                    StandardCharsets.UTF_8);
            String[] credentials = decoded.split(":", 2);
            if (credentials.length != 2) {
                throw new MessagingException("Invalid HTTP Basic credentials in STOMP CONNECT.");
            }
            AuthenticationManager manager = authenticationManager.getIfAvailable();
            if (manager == null) {
                throw new MessagingException("No AuthenticationManager is configured.");
            }
            return manager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(credentials[0], credentials[1]));
        } catch (AuthenticationException exception) {
            throw new MessagingException("STOMP authentication failed.", exception);
        } catch (IllegalArgumentException exception) {
            throw new MessagingException("Invalid Base64 credentials in STOMP CONNECT.", exception);
        }
    }
}
