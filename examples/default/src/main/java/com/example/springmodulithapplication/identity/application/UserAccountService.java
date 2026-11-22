package com.example.springmodulithapplication.identity.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springmodulithapplication.identity.IdentityProperties;
import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.identity.domain.UserAccount;
import com.example.springmodulithapplication.identity.domain.UserAccountRepository;
import com.example.springmodulithapplication.identity.domain.UserRole;
import com.example.springmodulithapplication.identity.events.UserRegisteredEvent;
import com.example.springmodulithapplication.shared.error.ConflictException;
import com.example.springmodulithapplication.shared.error.DomainValidationException;
import com.example.springmodulithapplication.shared.error.NotFoundException;

/**
 * Application service and implementation of the module's public API.
 */
@Service
public class UserAccountService implements IdentityApi {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,64}");

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;
    private final ApplicationEventPublisher events;

    public UserAccountService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            IdentityProperties properties,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.events = events;
    }

    @Override
    @Transactional
    public UserProfile register(RegisterUserCommand command) {
        if (!properties.isRegistrationEnabled()) {
            throw new DomainValidationException("Self-registration is disabled.");
        }

        String username = normalizeUsername(command.username());
        String email = normalizeEmail(command.email());
        String rawPassword = command.rawPassword();

        if (rawPassword == null || rawPassword.length() < properties.getMinimumPasswordLength()) {
            throw new DomainValidationException(
                    "Password must be at least " + properties.getMinimumPasswordLength() + " characters long.");
        }
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already registered.");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email address is already registered.");
        }

        Instant registeredAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        UserAccount account = new UserAccount(
                UUID.randomUUID(),
                username,
                email,
                passwordEncoder.encode(rawPassword),
                UserRole.USER,
                registeredAt);
        repository.save(account);

        events.publishEvent(new UserRegisteredEvent(
                account.getId(), account.getUsername(), account.getEmail(), registeredAt));

        return toProfile(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return repository.findByUsernameIgnoreCase(normalizeUsername(username)).map(this::toProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile requireById(UUID userId) {
        return repository.findById(userId)
                .map(this::toProfile)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found."));
    }

    private UserProfile toProfile(UserAccount account) {
        return new UserProfile(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getRole().name(),
                account.getCreatedAt());
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new DomainValidationException("Username must not be blank.");
        }
        String normalized = username.trim();
        if (!USERNAME.matcher(normalized).matches()) {
            throw new DomainValidationException(
                    "Username must be 3-64 characters and contain only letters, digits, dots, underscores, or hyphens.");
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainValidationException("Email must not be blank.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 320 || normalized.indexOf('@') <= 0 || normalized.endsWith("@")) {
            throw new DomainValidationException("Email must be a valid address.");
        }
        return normalized;
    }
}
