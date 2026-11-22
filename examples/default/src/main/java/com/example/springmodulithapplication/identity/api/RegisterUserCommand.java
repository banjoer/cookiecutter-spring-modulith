package com.example.springmodulithapplication.identity.api;

/**
 * Input for the public registration use case. The raw password is never stored or returned.
 */
public record RegisterUserCommand(
        String username,
        String email,
        String rawPassword) {
}
