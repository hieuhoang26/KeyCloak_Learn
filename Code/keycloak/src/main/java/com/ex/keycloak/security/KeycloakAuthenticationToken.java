package com.ex.keycloak.security;

import com.ex.keycloak.security.authentication.UserDetailsAdapter;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
public class KeycloakAuthenticationToken extends AbstractAuthenticationToken {

    private final String keycloakId;
    private final Object principal;

    /** Pre-authentication constructor: carries only the keycloakId, not yet authenticated. */
    public KeycloakAuthenticationToken(String keycloakId) {
        super(List.of());
        this.principal = keycloakId;
        this.keycloakId = keycloakId;
        setAuthenticated(false);
    }

    /** Post-authentication constructor: carries full UserDetailsAdapter, marked authenticated. */
    public KeycloakAuthenticationToken(String keycloakId, UserDetailsAdapter userDetails) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.keycloakId = keycloakId;
        this.principal = userDetails;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    /** Returns the keycloakId when in pre-auth state; the username when fully authenticated. */
    @Override
    public String getName() {
        return keycloakId;
    }
}
