package com.ex.keycloak.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class KeycloakAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    /** Pre-authentication constructor: carries only the keycloakId, not yet authenticated. */
    public KeycloakAuthenticationToken(String keycloakId) {
        super(List.of());
        this.principal = keycloakId;
        setAuthenticated(false);
    }

    /** Post-authentication constructor: carries full UserDetailsAdapter, marked authenticated. */
    public KeycloakAuthenticationToken(UserDetailsAdapter userDetails) {
        super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
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
        if (principal instanceof UserDetailsAdapter adapter) {
            return adapter.getUser().getKeycloakId();
        }
        return (String) principal;
    }
}
