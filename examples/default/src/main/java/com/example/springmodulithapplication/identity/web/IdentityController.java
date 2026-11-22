package com.example.springmodulithapplication.identity.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springmodulithapplication.identity.api.IdentityApi;
import com.example.springmodulithapplication.identity.api.RegisterUserCommand;
import com.example.springmodulithapplication.identity.api.UserProfile;
import com.example.springmodulithapplication.shared.error.NotFoundException;

@RestController
@RequestMapping("/api/identity")
public class IdentityController {

    private final IdentityApi identity;

    public IdentityController(IdentityApi identity) {
        this.identity = identity;
    }

    @PostMapping("/users")
    public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterUserRequest request) {
        UserProfile profile = identity.register(
                new RegisterUserCommand(request.username(), request.email(), request.password()));
        return ResponseEntity.created(URI.create("/api/identity/users/" + profile.id())).body(profile);
    }

    @GetMapping("/me")
    public UserProfile me(Authentication authentication) {
        return identity.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user no longer exists."));
    }

    @GetMapping("/users/{id}")
    public UserProfile byId(@PathVariable UUID id) {
        return identity.requireById(id);
    }
}
