# Phase 5: Spring Boot Integration

> **Goal:** Wire oauth2-proxy to a real Spring Boot backend. Implement both integration approaches — header trust (simple) and JWT validation (secure) — and understand when to use each.

---

## The two options at a glance

```
Option A: Trust headers (simple)
  Browser → oauth2-proxy → Spring Boot
                           reads X-Forwarded-Email
                           trusts it as the user's identity

Option B: Validate JWT (secure)
  Browser → oauth2-proxy → Spring Boot
                           reads Authorization: Bearer <token>
                           validates the JWT signature itself
```

**Use Option A** when: oauth2-proxy and your backend are in the same trusted network (e.g., same Kubernetes cluster, backend has no external exposure).

**Use Option B** when: your backend is also exposed directly (e.g., a public API consumed by mobile apps), or you need fine-grained claim-based authorization at the Spring level.

Most internal microservice setups start with Option A.

---

## Option A: Trust X-Forwarded headers

### How it works

oauth2-proxy validates the session and injects:
```
X-Forwarded-User:  <subject>
X-Forwarded-Email: user@example.com
X-Forwarded-Groups: engineering,admin
```

Your Spring Boot app reads these headers and maps them to your own user model. You trust them because network isolation ensures only oauth2-proxy can set them.

### oauth2-proxy config for Option A

No special config needed beyond what Phase 4 covered. Ensure `--set-xauthrequest=true` is set.

### Step 1: Add the filter

Create a `SecurityHeaderFilter` that reads the injected headers and populates Spring Security's `SecurityContext`:

```java
package com.example.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProxyAuthHeaderFilter extends OncePerRequestFilter {

    private static final String HEADER_USER  = "X-Forwarded-User";
    private static final String HEADER_EMAIL = "X-Forwarded-Email";
    private static final String HEADER_GROUPS = "X-Forwarded-Groups";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String user  = request.getHeader(HEADER_USER);
        String email = request.getHeader(HEADER_EMAIL);
        String groups = request.getHeader(HEADER_GROUPS);  // comma-separated

        if (user != null && email != null) {
            List<GrantedAuthority> authorities = parseGroups(groups);

            // Create an authenticated principal
            ProxyAuthUser principal = new ProxyAuthUser(user, email, groups);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> parseGroups(String groups) {
        if (groups == null || groups.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groups.split(","))
            .map(String::trim)
            .map(g -> new SimpleGrantedAuthority("ROLE_" + g.toUpperCase()))
            .collect(Collectors.toList());
    }
}
```

### Step 2: Create the principal object

```java
package com.example.security;

public record ProxyAuthUser(String subject, String email, String groups) {}
```

### Step 3: Configure Spring Security

```java
package com.example.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ProxyAuthHeaderFilter proxyAuthHeaderFilter;

    public SecurityConfig(ProxyAuthHeaderFilter proxyAuthHeaderFilter) {
        this.proxyAuthHeaderFilter = proxyAuthHeaderFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // No CSRF needed — sessions are managed by oauth2-proxy, not Spring
            .csrf(csrf -> csrf.disable())

            // Stateless — no Spring session (sessions live in oauth2-proxy)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Add our header-reading filter
            .addFilterBefore(proxyAuthHeaderFilter,
                UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

### Step 4: Use the identity in controllers

```java
@RestController
public class UserController {

    @GetMapping("/me")
    public Map<String, String> getCurrentUser(Authentication auth) {
        ProxyAuthUser user = (ProxyAuthUser) auth.getPrincipal();
        return Map.of(
            "subject", user.subject(),
            "email",   user.email()
        );
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ENGINEERING')")
    public String adminPage() {
        return "Welcome, engineer!";
    }
}
```

### Step 5: Testing locally (without oauth2-proxy)

In development, you don't want to run oauth2-proxy just to test endpoints. Create a test filter that simulates the headers:

```java
package com.example.security;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

// Only active when "dev" profile is enabled
@Component
@Profile("dev")
@Order(1)
public class DevMockHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Wrap the request to add mock headers
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return switch (name) {
                    case "X-Forwarded-User"   -> "dev-user-123";
                    case "X-Forwarded-Email"  -> "dev@example.com";
                    case "X-Forwarded-Groups" -> "engineering,users";
                    default -> super.getHeader(name);
                };
            }
        };

        filterChain.doFilter(wrapped, response);
    }
}
```

Run with `--spring.profiles.active=dev` for local development.

---

## Option B: JWT validation

### How it works

oauth2-proxy forwards the Access Token (a JWT) to your backend in the `Authorization` header. Your Spring Boot app validates the JWT signature using the Authorization Server's public keys.

**oauth2-proxy config for Option B:**

```
--pass-authorization-header=true
--pass-access-token=true
```

### Step 1: Add Spring Security OAuth2 Resource Server

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Step 2: Configure the JWKS endpoint

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Spring fetches public keys from this URL to validate tokens
          jwk-set-uri: https://keycloak.example.com/realms/myrealm/protocol/openid-connect/certs
```

Spring Security will automatically:
1. Intercept requests with `Authorization: Bearer <token>`
2. Fetch public keys from the JWKS URI
3. Validate the JWT signature, expiry, and issuer
4. Populate the `SecurityContext` with claims from the token

### Step 3: Configure authorization

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        // Map "groups" claim from Keycloak → Spring ROLE_ authorities
        grantedAuthoritiesConverter.setAuthoritiesClaimName("groups");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
```

### Step 4: Access claims in controllers

```java
@RestController
public class ProfileController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "subject", jwt.getSubject(),
            "email",   jwt.getClaimAsString("email"),
            "groups",  jwt.getClaimAsStringList("groups")
        );
    }
}
```

---

## Option A vs Option B — decision guide

| Concern | Option A (headers) | Option B (JWT) |
|---|---|---|
| Complexity | Low | Medium |
| Backend exposed externally? | Must not be | Safe either way |
| Token validation in backend | No | Yes (cryptographic) |
| Works without oauth2-proxy in dev? | Yes (mock headers) | Yes (Postman with real token) |
| Revocation on token expiry | Immediate (proxy checks session) | Delayed (until JWT expiry) |
| Fine-grained claim-based authz | Via groups header | Full JWT claims available |

---

## End-to-end docker-compose for Option A

```yaml
version: "3.8"

services:

  keycloak:
    image: quay.io/keycloak/keycloak:latest
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8081:8080"
    networks:
      - internal

  spring-backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: default
    networks:
      - internal
    # No ports exposed to host — only oauth2-proxy can reach it

  oauth2-proxy:
    image: quay.io/oauth2-proxy/oauth2-proxy:latest
    command:
      - --provider=oidc
      - --oidc-issuer-url=http://keycloak:8080/realms/demo
      - --client-id=${CLIENT_ID}
      - --client-secret=${CLIENT_SECRET}
      - --cookie-secret=${COOKIE_SECRET}
      - --upstream=http://spring-backend:8080
      - --redirect-url=http://localhost:4180/oauth2/callback
      - --http-address=0.0.0.0:4180
      - --email-domain=*
      - --cookie-secure=false
      - --set-xauthrequest=true
    ports:
      - "4180:4180"
    networks:
      - internal
    depends_on:
      - keycloak
      - spring-backend

networks:
  internal:
    driver: bridge
```

---

## Security checklist for Option A

- [ ] Backend service is NOT exposed outside the cluster/network
- [ ] Backend strips or ignores forwarded headers from non-proxy sources
- [ ] Ingress/load balancer strips `X-Forwarded-*` headers from incoming public requests
- [ ] `--cookie-secure=true` in production
- [ ] `--cookie-domain` set correctly for your domain

---

**Previous:** [Phase 4 — Deep Dive into Configuration](./phase-04-deep-dive-configuration.md)
**Next:** Phase 6 — Production Concepts *(coming soon)*
