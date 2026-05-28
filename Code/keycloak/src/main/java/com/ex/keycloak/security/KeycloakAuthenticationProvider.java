package com.ex.keycloak.security;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class KeycloakAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    public KeycloakAuthenticationProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String keycloakId = authentication.getName();

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UsernameNotFoundException("No local user for keycloakId: " + keycloakId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is not active: " + user.getStatus());
        }

        return new KeycloakAuthenticationToken(new UserDetailsAdapter(user));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return KeycloakAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
