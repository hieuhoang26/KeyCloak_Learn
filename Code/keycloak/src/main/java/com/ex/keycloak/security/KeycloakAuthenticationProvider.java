package com.ex.keycloak.security;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.repository.UserRepository;
import com.ex.keycloak.security.authentication.UserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
public class KeycloakAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    public KeycloakAuthenticationProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("Processing keycloak header auth");

        if(!(authentication instanceof KeycloakAuthenticationToken authToken)){
            log.debug("Auth token is not KeycloakAuthenticationToken");
            return null;
        }

        String keycloakId = authentication.getName();  // keycloakId

        if(keycloakId == null || keycloakId.trim().isEmpty()){
            throw new BadCredentialsException("Header is required");
        }
        try {
            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new UsernameNotFoundException("No local user for keycloakId: " + keycloakId));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("User account is not active: " + user.getStatus());
            }

            return new KeycloakAuthenticationToken(new UserDetailsAdapter(user));

        } catch (UsernameNotFoundException e){
            log.warn("User not found with keycloakId {}", keycloakId);
            throw new BadCredentialsException("User invalid",e);
        } catch (DisabledException e){
            log.warn("User disabled for keycloak Id {}", keycloakId);
            throw new BadCredentialsException("User invalid",e);
        } catch (Exception e){
            log.warn("Auth failed");
            throw new BadCredentialsException("User invalid",e);
        }


    }

    @Override
    public boolean supports(Class<?> authentication) {
        return KeycloakAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
