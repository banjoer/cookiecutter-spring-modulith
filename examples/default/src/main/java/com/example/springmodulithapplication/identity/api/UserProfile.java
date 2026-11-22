package com.example.springmodulithapplication.identity.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Public, immutable view of an identity user.
 */
public record UserProfile(
        UUID id,
        String username,
        String email,
        String role,
        Instant createdAt) {
}
