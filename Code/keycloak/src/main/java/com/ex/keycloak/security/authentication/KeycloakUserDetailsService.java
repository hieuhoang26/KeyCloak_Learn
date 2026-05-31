package com.ex.keycloak.security.authentication;

import com.ex.keycloak.dto.UserInfo;
import com.ex.keycloak.repository.UserRepository;
import com.ex.keycloak.security.UserDetailsAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }

    private UserDetailsAdapter loadUserByKeyCloakId(String keycloakId){
        log.debug("Loading user details for keycloak Id {}", keycloakId);

        if(keycloakId == null || keycloakId.trim().isEmpty()){
            throw new BadCredentialsException("Header is required");
        }

        UserInfo user  = userRepository.findUserInfoByKeycloakId(keycloakId.trim())
                .orElseThrow(() ->  new UsernameNotFoundException("User not found or disabled"));

        return new UserDetailsAdapter(user);

    }
}
