package com.example.springmodulithapplication.infrastructure.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless security foundation. There are no hardcoded users or secrets: credentials come from
 * the identity module (BCrypt hashes in PostgreSQL) or from the replacement authentication
 * mechanism you integrate here.
 *
 * <p>This is a starter, not a complete enterprise security policy. Replace HTTP Basic with OIDC,
 * mTLS, or signed tokens, and add fine-grained authorization rules for your domain.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/identity/users").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Builds an explicit AuthenticationManager so Boot does not create a random in-memory user.
     * When the identity module is disabled, authentication is still fails-closed rather than open.
     */
    @Bean
    AuthenticationManager authenticationManager(
            ObjectProvider<UserDetailsService> userDetailsServices,
            ObjectProvider<PasswordEncoder> passwordEncoders) {
        UserDetailsService userDetailsService = userDetailsServices.getIfAvailable(
                () -> username -> {
                    throw new UsernameNotFoundException("No identity user store is active.");
                });
        PasswordEncoder passwordEncoder = passwordEncoders.getIfAvailable(BCryptPasswordEncoder::new);

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
