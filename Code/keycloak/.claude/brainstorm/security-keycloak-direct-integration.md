# Keycloak Direct Integration (Without Istio / oauth2-proxy)

Replaces the proxy-injected `x-forwarded-smaile-user` header with in-app JWT validation against Keycloak's JWKS. Everything downstream (DB lookup, permissions, RBAC) is unchanged.

---

## What Changes

| Layer | With proxy | Without proxy |
|---|---|---|
| JWT validation | oauth2-proxy | Spring Boot validates against Keycloak JWKS |
| Keycloak ID source | `x-forwarded-smaile-user` header | JWT `sub` claim |
| `x-workspace-org-id` | sent by frontend | still sent by frontend — no change |
| DB lookup, permissions, RBAC | unchanged | unchanged |

---

## Request Flow

```
Frontend sends:
  Authorization: Bearer <keycloak-jwt>
  x-workspace-org-id: <org-uuid>
      │
      ▼
[KeycloakJwtPreAuthFilter]
  - Decodes JWT against Keycloak JWKS (auto-fetched from issuer-uri)
  - Extracts sub → sets as request attribute "smaile.keycloak.id"
      │
      ▼
[SmaileAuthenticationFilter]
  - Reads "smaile.keycloak.id" from request attribute  ← only change
  - Reads x-workspace-org-id header                    ← unchanged
  - Creates SmaileAuthenticationToken                  ← unchanged
      │
      ▼
[SmaileAuthenticationProvider]    ← unchanged
  - DB lookup by keycloakId
  - Checks enabled/locked/expired
      │
      ▼
[SecurityContext]                 ← unchanged
  SmaileUserDetails with org contexts + permissions
      │
      ▼
[Controller + AOP]                ← unchanged
  @TagResourcePermission / @PreAuthorize
```

---

## Implementation

### Step 1 — Add Dependency

`pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Step 2 — Configure Keycloak Issuer

`application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${IAM_ENDPOINT}/realms/${IAM_REALM}
```

Spring auto-fetches the JWKS from `{issuer-uri}/.well-known/openid-configuration`.

Local values: `IAM_ENDPOINT=http://localhost:8080`, `IAM_REALM=smaile`
→ issuer: `http://localhost:8080/realms/smaile`

### Step 3 — Create `KeycloakJwtPreAuthFilter`

`security/authentication/KeycloakJwtPreAuthFilter.java`:
```java
@Component
@RequiredArgsConstructor
public class KeycloakJwtPreAuthFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public static final String KEYCLOAK_ID_ATTR = "smaile.keycloak.id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Jwt jwt = jwtDecoder.decode(token);
                request.setAttribute(KEYCLOAK_ID_ATTR, jwt.getSubject());
            } catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

### Step 4 — Update `SmaileAuthenticationFilter`

One change: fall back to the request attribute when the proxy header is absent.

```java
// Before:
String keycloakId = request.getHeader("x-forwarded-smaile-user");

// After:
String keycloakId = request.getHeader("x-forwarded-smaile-user");
if (keycloakId == null) {
    keycloakId = (String) request.getAttribute(KeycloakJwtPreAuthFilter.KEYCLOAK_ID_ATTR);
}

if (keycloakId == null) {
    throw new InsufficientAuthenticationException("Authentication required");
}
```

`x-workspace-org-id` extraction is unchanged.

### Step 5 — Update `WebSecurityConfig`

Register the `JwtDecoder` bean and add the pre-filter before `SmaileAuthenticationFilter`:

```java
@Bean
public JwtDecoder jwtDecoder() {
    return JwtDecoders.fromIssuerLocation(
        iamProperties.getEndpoint() + "/realms/" + iamProperties.getRealm());
}

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    SmaileAuthenticationFilter authFilter =
            new SmaileAuthenticationFilter(authenticationManager(), contextPath);

    http
        // ... existing cors, csrf, exceptionHandling ...
        .addFilterBefore(keycloakJwtPreAuthFilter, SmaileAuthenticationFilter.class)
        .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(WHITELIST).permitAll()
                .anyRequest().authenticated())
        .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
}
```

---

## Keycloak Client Requirements

In Keycloak admin for the `smaile-be` client:
- **Access Type**: `confidential` (service-to-service) or `public` with PKCE (frontend SPA)
- **Standard Flow**: enabled
- **Valid Redirect URIs**: frontend URLs
- JWT `sub` claim = Keycloak internal user UUID → must match `keycloakId` column in `User` table

No Keycloak realm config changes needed if user IDs are already synced.

---

## Compatibility Note

The fallback order in `SmaileAuthenticationFilter` means both modes work simultaneously:
1. **With proxy**: `x-forwarded-smaile-user` header is present → used directly
2. **Without proxy**: header absent → reads from `smaile.keycloak.id` request attribute set by `KeycloakJwtPreAuthFilter`

This allows a gradual migration without breaking existing deployments.
