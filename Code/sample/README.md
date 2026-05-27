# Keycloak Terraform — Spring Boot + JWT + Role-Based Auth

Provisions a complete Keycloak realm with:

- **Realm** with security hardening (brute-force protection, password policy, CSP headers)
- **Backend client** (confidential) for Spring Boot resource server JWT validation
- **Frontend client** (public) with Authorization Code + PKCE for SPAs
- **Role hierarchy**: `admin` → `manager` → `user` → `viewer` (composite roles)
- **JWT protocol mappers** that embed `roles` claim in access tokens
- **Audience mapper** so frontend tokens target the backend API
- **Demo users** with pre-assigned roles for immediate testing

---

## Quick Start

```bash
# 1. Start Keycloak locally
docker compose up -d

# 2. Wait for health check to pass (~30s)
docker compose logs -f keycloak | grep -m1 "started"

# 3. Configure Terraform
cp terraform.tfvars.example terraform.tfvars
# Edit secrets as needed

# 4. Apply
terraform init
terraform plan
terraform apply
```

---

## Architecture

```
┌────────────────┐   Auth Code + PKCE   ┌──────────────┐
│  Frontend SPA  │ ◄──────────────────► │   Keycloak   │
│  (public)      │                       │   Realm      │
└───────┬────────┘                       └──────┬───────┘
        │ Bearer JWT                            │
        ▼                                       │ JWT verify
┌────────────────┐                              │ (JWKS)
│  Spring Boot   │ ◄────────────────────────────┘
│  API (conf.)   │
└────────────────┘
```

---

## JWT Token Structure

After applying, access tokens will contain:

```json
{
  "iss": "http://localhost:8080/realms/app-realm",
  "aud": ["spring-boot-api"],
  "sub": "user-uuid",
  "preferred_username": "admin-user",
  "email": "admin@example.com",
  "roles": ["admin", "manager", "user", "viewer"],
  "scope": "openid profile email app-roles"
}
```

---

## Spring Boot Integration

### application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/app-realm
          jwk-set-uri: http://localhost:8080/realms/app-realm/protocol/openid-connect/certs
```

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("admin")
                .requestMatchers("/api/manage/**").hasAnyRole("admin", "manager")
                .requestMatchers("/api/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthorities = new JwtGrantedAuthoritiesConverter();
        // Map the "roles" claim (set by our Terraform mapper) to Spring authorities
        grantedAuthorities.setAuthoritiesClaimName("roles");
        grantedAuthorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthorities);
        return converter;
    }
}
```

### Controller with role-based access

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/health")
    public String health() { return "OK"; }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('user')")
    public Map<String, Object> profile(JwtAuthenticationToken token) {
        return Map.of(
            "username", token.getToken().getClaimAsString("preferred_username"),
            "roles", token.getToken().getClaimAsStringList("roles")
        );
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('admin')")
    public String adminOnly() { return "Admin access granted"; }

    @GetMapping("/manage/reports")
    @PreAuthorize("hasAnyRole('admin', 'manager')")
    public String managerAccess() { return "Manager access granted"; }
}
```

---

## Frontend Integration (React + oidc-client-ts)

```typescript
// authConfig.ts
import { UserManager, WebStorageStateStore } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: "http://localhost:8080/realms/app-realm",
  client_id: "frontend-app",
  redirect_uri: "http://localhost:3000/callback",
  post_logout_redirect_uri: "http://localhost:3000",
  scope: "openid profile email app-roles",
  response_type: "code",
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
});

// Helper to get roles from the token
export function getUserRoles(user: User): string[] {
  return user?.profile?.roles ?? [];
}

// Role-checking helper
export function hasRole(user: User, role: string): boolean {
  return getUserRoles(user).includes(role);
}
```

```tsx
// ProtectedRoute.tsx
function ProtectedRoute({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" />;
  if (role && !hasRole(user, role)) return <Navigate to="/forbidden" />;
  return children;
}

// Usage in routes
<Route path="/admin" element={
  <ProtectedRoute role="admin"><AdminPage /></ProtectedRoute>
} />
```

---

## Files

| File | Purpose |
|------|---------|
| `providers.tf` | Keycloak provider config |
| `variables.tf` | All configurable inputs |
| `main.tf` | Realm, clients, roles, mappers, users |
| `outputs.tf` | OIDC endpoints, config snippets |
| `terraform.tfvars.example` | Example variable values |
| `docker-compose.yml` | Local Keycloak instance |

---

## Demo Users

| Username | Password | Roles |
|----------|----------|-------|
| `admin-user` | `admin123` | admin, manager, user |
| `manager-user` | `manager123` | manager, user |
| `regular-user` | `user123` | user |
| `viewer-user` | `viewer123` | viewer |

> Set `create_demo_users = false` in production.

---

## Useful Commands

```bash
# View outputs after apply
terraform output -json

# Get backend client secret
terraform output -raw backend_client_secret

# Get a test token via password grant (dev only)
curl -s -X POST "http://localhost:8080/realms/app-realm/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=frontend-app" \
  -d "username=admin-user" \
  -d "password=admin123" \
  -d "scope=openid" | jq -r '.access_token' | cut -d. -f2 | base64 -d | jq

# Destroy everything
terraform destroy
```
