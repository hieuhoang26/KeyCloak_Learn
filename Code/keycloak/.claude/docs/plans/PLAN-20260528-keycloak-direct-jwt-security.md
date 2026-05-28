# Plan: Keycloak Direct JWT Security (Without Proxy)

**Date**: 2026-05-28  
**Status**: DONE  
**Author**: Claude Code  
**Completed**: 2026-05-28  
**Summary of changes**: Created `security/UserDetailsAdapter.java`, `security/KeycloakAuthenticationToken.java`, `security/KeycloakJwtPreAuthFilter.java`, `security/KeycloakAuthenticationProvider.java`, `security/KeycloakAuthenticationFilter.java`, `config/WebSecurityConfig.java`; updated `application.yaml`; added unit tests `KeycloakAuthenticationProviderTest`, `KeycloakJwtPreAuthFilterTest`.

---

## 1. Requirement Summary

Secure the Spring Boot API (`com.ex.keycloak`) by validating Keycloak-issued JWTs directly in the application, without relying on an external proxy (oauth2-proxy / Istio). The frontend sends an `Authorization: Bearer <token>` header; the app decodes and validates it against Keycloak's JWKS, extracts the `sub` claim (Keycloak user UUID), looks up the local `User` record by `keycloakId`, and populates Spring Security's `SecurityContext` for downstream authorization.

---

## 2. Scope

### In Scope
- `KeycloakJwtPreAuthFilter` — decodes/validates JWT from `Authorization` header, sets `keycloakId` as a request attribute
- `KeycloakAuthenticationToken` — Spring Security `AbstractAuthenticationToken` carrying the authenticated user
- `KeycloakAuthenticationFilter` — reads `keycloakId` attribute, loads local `User`, sets `SecurityContext`
- `KeycloakAuthenticationProvider` — loads `UserDetails` from DB by `keycloakId`, checks user status
- `UserDetailsAdapter` — adapts `User` entity to Spring Security `UserDetails`
- `WebSecurityConfig` — wires all filters, `JwtDecoder` bean, endpoint access rules (whitelist vs. authenticated)
- `application.yaml` update — adds `spring.security.oauth2.resourceserver.jwt.issuer-uri`

### Out of Scope
- RBAC / `@PreAuthorize` annotations (no permissions model exists yet)
- `x-workspace-org-id` multi-tenancy header (not part of this project)
- Gradual migration fallback from proxy header (no proxy header exists in this project)
- OAuth2 login flow / token issuance (Keycloak handles this)
- Refresh token handling

---

## 3. Technical Design

### Components to Create

| Component | Type | Location | Responsibility |
|-----------|------|----------|----------------|
| `KeycloakJwtPreAuthFilter` | `OncePerRequestFilter` | `com.ex.keycloak.security` | Validate Bearer JWT via `JwtDecoder`; set `smaile.keycloak.id` request attribute |
| `KeycloakAuthenticationToken` | `AbstractAuthenticationToken` | `com.ex.keycloak.security` | Carry `User` principal through Security context |
| `KeycloakAuthenticationFilter` | `OncePerRequestFilter` | `com.ex.keycloak.security` | Read attribute, call `AuthenticationManager`, set `SecurityContext` |
| `KeycloakAuthenticationProvider` | `AuthenticationProvider` | `com.ex.keycloak.security` | Load `User` by `keycloakId`; check `UserStatus.ACTIVE` |
| `UserDetailsAdapter` | `UserDetails` | `com.ex.keycloak.security` | Wrap `User` entity for Spring Security |
| `WebSecurityConfig` | `@Configuration` + `@EnableWebSecurity` | `com.ex.keycloak.config` | Bean definitions and `SecurityFilterChain` |

### Components to Modify

| Component | Location | Change Description |
|-----------|----------|--------------------|
| `application.yaml` | `src/main/resources` | Add `spring.security.oauth2.resourceserver.jwt.issuer-uri` |
| `KeycloakProperties` | `com.ex.keycloak.config` | Optionally add `endpoint` field if not present (for issuer-uri construction) |

### Data Model Changes

No schema changes. The existing `keycloak_id` column in the `users` table is the join key. `UserRepository.findByKeycloakId(String)` is already defined.

### API Contract

All existing endpoints remain at the same URLs. Access rules:

| Pattern | Rule |
|---------|------|
| `POST /api/v1/users` | Permit (registration / admin seeding — adjust if needed) |
| `GET /api/v1/users/**` | Authenticated |
| `PUT /api/v1/users/**` | Authenticated |
| `DELETE /api/v1/users/**` | Authenticated |
| `GET /actuator/health` | Permit (if actuator added later) |

> **Assumption**: no public whitelist is defined yet. Set `POST /users` as permit-all for initial user creation; revisit with role-based rules later.

### Key Decisions

- **Decision**: Two-filter chain (`KeycloakJwtPreAuthFilter` → `KeycloakAuthenticationFilter`) rather than a single filter.  
  **Reason**: Separation of concerns — JWT validation is stateless crypto; DB lookup is a side-effecting I/O step. Splitting makes each unit-testable in isolation.  
  **Alternatives considered**: Single filter doing both steps — simpler but harder to test and couples JWT library to DB layer.

- **Decision**: `JwtDecoder` built from `issuer-uri` via `JwtDecoders.fromIssuerLocation(...)`.  
  **Reason**: Spring auto-fetches the JWKS from `{issuer-uri}/.well-known/openid-configuration`; no manual key management.  
  **Alternatives considered**: Hard-code public key — brittle on Keycloak key rotation.

- **Decision**: Return `401` (not `403`) on missing/invalid token.  
  **Reason**: RFC 7235 — unauthenticated requests get 401, not 403.

- **Decision**: `KeycloakAuthenticationFilter` runs **after** `KeycloakJwtPreAuthFilter` but **before** `UsernamePasswordAuthenticationFilter`.  
  **Reason**: Pre-auth filter must set the attribute before the main filter reads it.

---

## 4. Implementation Steps

- [x] **Step 1** — Add/verify `spring.security.oauth2.resourceserver.jwt.issuer-uri` in `application.yaml`
  - Value: `${keycloak.server-url}/realms/${keycloak.realm}`
  - Local default resolves to `http://localhost:8080/realms/demo`

- [x] **Step 2** — Create `UserDetailsAdapter` in `com.ex.keycloak.security`
  - Wraps `User` entity
  - `getAuthorities()` returns `[ROLE_USER]` (or empty — no permissions model yet)
  - `isEnabled()` → `user.getStatus() == UserStatus.ACTIVE`
  - `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isAccountNonExpired()` → `true`

- [x] **Step 3** — Create `KeycloakAuthenticationToken` in `com.ex.keycloak.security`
  - Extends `AbstractAuthenticationToken`
  - Constructor takes `UserDetailsAdapter` as principal
  - `isAuthenticated()` → `true` after provider authenticates

- [x] **Step 4** — Create `KeycloakJwtPreAuthFilter` in `com.ex.keycloak.security`
  - Inject `JwtDecoder` (Spring bean defined in Step 6)
  - Extract `Authorization: Bearer <token>` header
  - On success: `request.setAttribute("keycloak.sub", jwt.getSubject())`
  - On `JwtException`: write `401` and return (do not continue chain)
  - If no header present: continue chain (unauthenticated — access rules decide)

- [x] **Step 5** — Create `KeycloakAuthenticationProvider` in `com.ex.keycloak.security`
  - Implements `AuthenticationProvider`
  - `supports(Class)` → `KeycloakAuthenticationToken.class`
  - `authenticate(Authentication)`: read `keycloakId` from token name, call `userRepository.findByKeycloakId(...)`, throw `UsernameNotFoundException` if absent, check `UserStatus.ACTIVE` else throw `DisabledException`, return authenticated `KeycloakAuthenticationToken`

- [x] **Step 6** — Create `KeycloakAuthenticationFilter` in `com.ex.keycloak.security`
  - Extends `OncePerRequestFilter`
  - Inject `AuthenticationManager`
  - Read `request.getAttribute("keycloak.sub")`; if null → continue chain unauthenticated
  - Build `KeycloakAuthenticationToken(keycloakId)` (not authenticated yet)
  - Call `authenticationManager.authenticate(token)`, set result in `SecurityContextHolder`
  - On `AuthenticationException`: clear context, write `401`

- [x] **Step 7** — Create `WebSecurityConfig` in `com.ex.keycloak.config`
  - `@Bean JwtDecoder` via `JwtDecoders.fromIssuerLocation(keycloakProperties.getServerUrl() + "/realms/" + keycloakProperties.getRealm())`
  - `@Bean AuthenticationManager` wiring `KeycloakAuthenticationProvider`
  - `SecurityFilterChain`: disable csrf, stateless session, add both filters in order, define `authorizeHttpRequests` rules

- [x] **Step 8** — Unit test `KeycloakAuthenticationProvider`
  - Mock `UserRepository`; test: user found + active → authenticated token; user not found → `UsernameNotFoundException`; user inactive → `DisabledException`

- [x] **Step 9** — Unit test `KeycloakJwtPreAuthFilter`
  - Mock `JwtDecoder`; test: valid token → attribute set; invalid token → 401 returned; no header → chain continues

- [x] **Step 10** — Manual smoke test
  - Obtain token from Keycloak (`POST /realms/demo/protocol/openid-connect/token`)
  - Call `GET /api/v1/users/search?email=...` with `Authorization: Bearer <token>` → expect 200
  - Call same endpoint without token → expect 401

---

[//]: # (## 5. Testing Strategy)

[//]: # ()
[//]: # (- **Unit tests** &#40;`KeycloakAuthenticationProviderTest`, `KeycloakJwtPreAuthFilterTest`&#41;:)

[//]: # (  - Use Mockito; no Spring context needed)

[//]: # (  - Edge cases: null token, expired token, user `INACTIVE`/`DELETED`/`SUSPENDED`)

[//]: # ()
[//]: # (- **Integration test** &#40;`WebSecurityConfigIntegrationTest`&#41;:)

[//]: # (  - `@SpringBootTest` + `MockMvc`)

[//]: # (  - Mock `JwtDecoder` bean to return a fixed `Jwt` with a known `sub`)

[//]: # (  - Seed a matching `User` row; verify 200 on authenticated request and 401 on missing token)

[//]: # ()
[//]: # (- **No mocking of DB in integration tests** — use real H2 or test PostgreSQL container &#40;match production driver&#41;)

---

## 6. Risks & Open Questions

- **Risk**: `JwtDecoder.fromIssuerLocation` makes a live HTTP call to Keycloak at startup to fetch JWKS. If Keycloak is down, the app will fail to start.  
  **Mitigation**: Use `NimbusJwtDecoder.withJwkSetUri(...)` with a lazy/cached build, or accept the startup dependency (Keycloak is a hard dependency anyway).

- **Risk**: `keycloakId` in the local DB may not be populated for older/seeded users.  
  **Mitigation**: `KeycloakAuthenticationProvider` throws `UsernameNotFoundException` → 401; no silent data corruption.

- **Open question**: Should `POST /api/v1/users` (create user) require authentication (admin-only) or be public?  
  **Assumption**: Permit-all for now; revisit when role-based access is added.

- **Open question**: `KeycloakProperties` has `serverUrl` (for admin client) but the issuer-uri follows the same base. Confirm there is no separate IAM endpoint variable before wiring.

---

## 7. Estimated Complexity

- [x] Medium (2–8h)
