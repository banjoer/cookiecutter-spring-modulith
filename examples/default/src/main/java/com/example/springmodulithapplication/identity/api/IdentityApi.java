package com.example.springmodulithapplication.identity.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API of the identity module.
 *
 * <p>Other modules must use this interface rather than the module's repositories, entities, or
 * services. The interface is part of the {@code identity :: api} named interface.</p>
 */
public interface IdentityApi {

    /**
     * Registers a user and publishes a {@code UserRegisteredEvent}.
     */
    UserProfile register(RegisterUserCommand command);

    /**
     * Looks up a user by the canonical username.
     */
    Optional<UserProfile> findByUsername(String username);

    /**
     * Returns a user or throws a {@code NotFoundException}.
     */
    UserProfile requireById(UUID userId);
}
