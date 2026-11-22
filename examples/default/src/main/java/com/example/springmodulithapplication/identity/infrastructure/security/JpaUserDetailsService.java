package com.example.springmodulithapplication.identity.infrastructure.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springmodulithapplication.identity.domain.UserAccount;
import com.example.springmodulithapplication.identity.domain.UserAccountRepository;

/**
 * Adapter between Spring Security and the identity persistence model.
 *
 * <p>The {@link UserDetailsService} interface is the only contract exposed to the security
 * configuration. No identity entity or repository leaks into the infrastructure module.</p>
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public JpaUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByUsernameIgnoreCase(username)
                .map(JpaUserDetailsService::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user."));
    }

    private static UserDetails toUserDetails(UserAccount account) {
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRole().name())
                .build();
    }
}
