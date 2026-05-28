package com.ex.keycloak.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class KeycloakAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    public KeycloakAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String keycloakId = (String) request.getAttribute(KeycloakJwtPreAuthFilter.KEYCLOAK_ID_ATTR);

        if (keycloakId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            KeycloakAuthenticationToken token = new KeycloakAuthenticationToken(keycloakId);
            KeycloakAuthenticationToken authenticated =
                    (KeycloakAuthenticationToken) authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(authenticated);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
