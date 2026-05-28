package com.ex.keycloak.security;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthenticationProviderTest {

    @Mock
    private UserRepository userRepository;

    private KeycloakAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new KeycloakAuthenticationProvider(userRepository);
    }

    @Test
    void authenticate_activeUser_returnsAuthenticatedToken() {
        User user = buildUser(UserStatus.ACTIVE);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        KeycloakAuthenticationToken input = new KeycloakAuthenticationToken("kc-123");
        Authentication result = provider.authenticate(input);

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(UserDetailsAdapter.class);
        UserDetailsAdapter adapter = (UserDetailsAdapter) result.getPrincipal();
        assertThat(adapter.getUser().getKeycloakId()).isEqualTo("kc-123");
    }

    @Test
    void authenticate_userNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        KeycloakAuthenticationToken input = new KeycloakAuthenticationToken("unknown");

        assertThatThrownBy(() -> provider.authenticate(input))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void authenticate_inactiveUser_throwsDisabledException() {
        User user = buildUser(UserStatus.INACTIVE);
        when(userRepository.findByKeycloakId("kc-456")).thenReturn(Optional.of(user));

        KeycloakAuthenticationToken input = new KeycloakAuthenticationToken("kc-456");

        assertThatThrownBy(() -> provider.authenticate(input))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void authenticate_deletedUser_throwsDisabledException() {
        User user = buildUser(UserStatus.DELETED);
        when(userRepository.findByKeycloakId("kc-789")).thenReturn(Optional.of(user));

        KeycloakAuthenticationToken input = new KeycloakAuthenticationToken("kc-789");

        assertThatThrownBy(() -> provider.authenticate(input))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void supports_keycloakAuthenticationToken_returnsTrue() {
        assertThat(provider.supports(KeycloakAuthenticationToken.class)).isTrue();
    }

    @Test
    void supports_otherToken_returnsFalse() {
        assertThat(provider.supports(Authentication.class)).isFalse();
    }

    private User buildUser(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .keycloakId("kc-123")
                .email("test@example.com")
                .username("testuser")
                .fullName("Test User")
                .status(status)
                .build();
    }
}
