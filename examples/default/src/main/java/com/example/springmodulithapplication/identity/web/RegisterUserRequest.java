package com.example.springmodulithapplication.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP input model. The service applies the configurable password policy in addition to these
 * structural constraints.
 */
public record RegisterUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 128) String password) {
}
