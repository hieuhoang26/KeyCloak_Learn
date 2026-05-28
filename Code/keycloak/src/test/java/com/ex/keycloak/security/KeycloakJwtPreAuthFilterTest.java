package com.ex.keycloak.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakJwtPreAuthFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private FilterChain filterChain;

    private KeycloakJwtPreAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new KeycloakJwtPreAuthFilter(jwtDecoder);
    }

    @Test
    void validToken_setsKeycloakIdAttributeAndContinuesChain() throws Exception {
        Jwt jwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .claim("sub", "user-uuid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(KeycloakJwtPreAuthFilter.KEYCLOAK_ID_ATTR)).isEqualTo("user-uuid-123");
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void invalidToken_returns401AndDoesNotContinueChain() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("expired"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void noAuthHeader_continuesChainUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(KeycloakJwtPreAuthFilter.KEYCLOAK_ID_ATTR)).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(anyString());
    }

    @Test
    void nonBearerAuthHeader_continuesChainUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute(KeycloakJwtPreAuthFilter.KEYCLOAK_ID_ATTR)).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(anyString());
    }
}
