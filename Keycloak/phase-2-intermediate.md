# Phase 2 — Intermediate: Configuration & App Integration

> **Timeline:** Months 2–3  
> **Goal:** Configure Keycloak for real-world use, secure a Spring Boot backend and a frontend SPA, and ship a custom login theme.

---

## 1. Realm & Client Configuration

### Realm Settings Checklist

| Setting                | Where                     | Recommended value                                 |
| ---------------------- | ------------------------- | ------------------------------------------------- |
| Display name           | Realm settings → General  | User-friendly app name                            |
| Login theme            | Realm settings → Themes   | Your custom theme (see section 5)                 |
| SSL required           | Realm settings → General  | `external requests` (dev) / `all requests` (prod) |
| Session timeout        | Realm settings → Sessions | 30 min SSO idle, 8 h SSO max                      |
| Access token lifespan  | Realm settings → Tokens   | 5 min (short; rely on refresh tokens)             |
| Refresh token lifespan | Realm settings → Sessions | Matches SSO session max                           |

### Client Configuration in Depth

For a **confidential backend client** (Spring Boot):

```
Client ID:               spring-api
Client authentication:   On  ← generates a client secret
Authorization:           Off (unless you need fine-grained authz)
Valid redirect URIs:     http://localhost:8081/*
Valid post logout URIs:  http://localhost:8081/logout
Web origins:             +  ← copies redirect URIs for CORS
```

For a **public SPA client** (React / Vue / Angular):

```
Client ID:               react-app
Client authentication:   Off
Standard flow:           On   ← authorization code + PKCE
Direct access grants:    Off  ← never enable for SPAs
Valid redirect URIs:     http://localhost:3000/*
Web origins:             http://localhost:3000
```

> **PKCE (Proof Key for Code Exchange):** Always enable for public clients. Keycloak enforces it automatically when `Client authentication` is Off. It prevents authorization code interception attacks.

---

## 2. Role-Based Access Control (RBAC)

### Defining Roles

Create realm roles for coarse-grained access, client roles for app-specific permissions.

```
Realm roles:
  user        ← default role assigned to all new users
  moderator
  admin       ← composite: user + moderator + admin-only permissions

Client roles (spring-api):
  read:products
  write:products
  delete:products
```

### Assigning Roles to Users

1. Go to **Users** → select user → **Role mapping** tab
2. Click **Assign role** → filter by realm or client
3. Select the role and click **Assign**

### Assigning Roles to Groups

1. Go to **Groups** → create group (e.g., `editors`)
2. Go to group → **Role mapping** → assign roles
3. Add users to the group — they inherit roles automatically

### Exposing Roles in Tokens

By default, realm roles appear in the token under `realm_access.roles`, and client roles under `resource_access.<client-id>.roles`.

```json
{
  "realm_access": {
    "roles": ["user", "moderator"]
  },
  "resource_access": {
    "spring-api": {
      "roles": ["write:products"]
    }
  }
}
```

To add roles as a flat `roles` claim, create a **Protocol Mapper**:

1. Client → **Client scopes** → `<client>-dedicated` → **Add mapper** → **By configuration**
2. Choose **User Realm Role** or **User Client Role**
3. Token claim name: `roles`, add to access token: **On**

---

## 3. User Federation

### Connecting LDAP / Active Directory

1. Go to **User federation** → **Add provider** → **LDAP**
2. Fill in connection details:

```
Connection URL:       ldap://your-ldap-server:389
Bind DN:              cn=admin,dc=example,dc=com
Bind credential:      <password>
Users DN:             ou=users,dc=example,dc=com
Username LDAP attr:   uid   (or sAMAccountName for AD)
RDN LDAP attr:        uid
UUID LDAP attr:       entryUUID   (or objectGUID for AD)
User object classes:  inetOrgPerson, organizationalPerson
```

3. Click **Test connection** then **Test authentication**
4. Set **Edit mode**:
   - `READ_ONLY` — Keycloak reads from LDAP, no writes back
   - `WRITEABLE` — Keycloak can update LDAP attributes
   - `UNSYNCED` — changes stay local to Keycloak

5. Click **Save** then **Synchronize all users**

### Identity Brokering (Social / External IdPs)

1. Go to **Identity providers** → choose provider (Google, GitHub, OIDC, SAML)
2. For Google:
   - Create OAuth credentials at [console.cloud.google.com](https://console.cloud.google.com)
   - Authorized redirect URI: `http://localhost:8080/realms/demo/broker/google/endpoint`
   - Paste Client ID and Client Secret into Keycloak

3. Under **First login flow**, choose what happens when a brokered user logs in for the first time:
   - **Create new user** — automatic account creation
   - **Link to existing account** — match by email and merge

---

## 4. Token Flows

### Authorization Code Flow (with PKCE)

Used by browser apps and native apps. The safest flow for user-facing clients.

```
1. App generates code_verifier and code_challenge
2. App redirects user to Keycloak /auth endpoint
3. User logs in; Keycloak redirects back with authorization code
4. App exchanges code + code_verifier for tokens (back-channel)
5. Keycloak returns access_token, id_token, refresh_token
```

### Client Credentials Flow

Used by machine-to-machine services (no user involved).

```bash
curl -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=client_credentials" \
  -d "client_id=service-account-client" \
  -d "client_secret=<secret>"
```

Returns an access token. The associated service account can be assigned roles just like a user.

### Refresh Token Flow

```bash
curl -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "grant_type=refresh_token" \
  -d "client_id=demo-app" \
  -d "refresh_token=<refresh_token>"
```

Returns a new access token and (optionally) a new refresh token. Implement this in your frontend to avoid forcing re-login when the access token expires.

> **Token lifespan recommendations:**
>
> - Access token: **5 minutes** — short lifespan limits damage from leakage
> - Refresh token: **30 minutes to 8 hours** — balances UX vs security
> - SSO session: **8 hours** — matches a working day for enterprise apps

---

## 5. Integrating with Spring Boot

### Dependencies (`pom.xml`)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Application Configuration (`application.yml`)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/demo
```

Keycloak's `/.well-known/openid-configuration` endpoint is auto-discovered from the issuer URI.

### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("realm_access.roles");
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

### Protecting an Endpoint

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    @PreAuthorize("hasRole('user')")
    public List<Product> list() { ... }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Product create(@RequestBody Product product) { ... }
}
```

Enable method security by adding `@EnableMethodSecurity` to your config class.

---

## 6. Integrating with a React Frontend

### Using `oidc-client-ts`

```bash
npm install oidc-client-ts react-oidc-context
```

### Auth provider setup (`main.tsx`)

```tsx
import { AuthProvider } from "react-oidc-context";

const oidcConfig = {
  authority: "http://localhost:8080/realms/demo",
  client_id: "react-app",
  redirect_uri: "http://localhost:3000/callback",
  scope: "openid profile email",
  post_logout_redirect_uri: "http://localhost:3000",
};

root.render(
  <AuthProvider {...oidcConfig}>
    <App />
  </AuthProvider>,
);
```

### Using auth in a component

```tsx
import { useAuth } from "react-oidc-context";

function Dashboard() {
  const auth = useAuth();

  if (auth.isLoading) return <div>Loading...</div>;
  if (!auth.isAuthenticated) {
    return <button onClick={() => auth.signinRedirect()}>Log in</button>;
  }

  return (
    <div>
      <p>Hello, {auth.user?.profile.name}</p>
      <button onClick={() => auth.signoutRedirect()}>Log out</button>
    </div>
  );
}
```

### Calling a protected API

```tsx
async function fetchProducts(auth) {
  const res = await fetch("http://localhost:8081/api/products", {
    headers: {
      Authorization: `Bearer ${auth.user?.access_token}`,
    },
  });
  return res.json();
}
```

---

## 7. Custom Login Theme

### Create the theme folder

```
themes/
└── my-theme/
    └── login/
        ├── theme.properties
        ├── login.ftl          ← overrides default login page
        └── resources/
            └── css/
                └── login.css
```

### `theme.properties`

```properties
parent=keycloak   # inherit everything not overridden
import=common/keycloak

styles=css/login.css
```

### Mount the theme in Docker

```bash
docker run -p 8080:8080 \
  -v $(pwd)/themes:/opt/keycloak/themes \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

### Activate the theme

1. Admin console → **Realm settings** → **Themes** tab
2. Login theme → select `my-theme`
3. Open an incognito window and navigate to your client's login URL to preview

---

## 8. Session & Token Lifespan for Production

### Recommended settings (Realm settings → Tokens)

| Setting                          | Value   | Reason                               |
| -------------------------------- | ------- | ------------------------------------ |
| Access token lifespan            | 5 min   | Short window if token is leaked      |
| Access token lifespan (implicit) | 15 min  | Slightly longer for legacy flows     |
| Client login timeout             | 2 min   | Time to complete the login form      |
| SSO session idle                 | 30 min  | Log out after inactivity             |
| SSO session max                  | 8 h     | Force re-login after a full day      |
| Offline session idle             | 30 days | For mobile apps using offline tokens |

### HTTPS for Production

Keycloak will refuse to issue tokens to non-HTTPS redirect URIs when `SSL required` is set to `all requests`. Always:

1. Terminate TLS at a reverse proxy (Nginx, Traefik, or a cloud load balancer)
2. Pass `X-Forwarded-Proto: https` to Keycloak
3. Start Keycloak with `--proxy edge` (trusts the proxy header)

```bash
./kc.sh start \
  --proxy edge \
  --hostname https://auth.yourdomain.com \
  --db postgres \
  --db-url jdbc:postgresql://db:5432/keycloak \
  --db-username keycloak \
  --db-password <password>
```

---

## 9. CORS Configuration

Keycloak handles CORS for its own endpoints. For your APIs:

1. Add the frontend origin to **Web origins** in the client config
2. In Spring Boot, allow the Authorization header:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

## 10. Action Steps Checklist

- [ ] Secure a Spring Boot API endpoint — verify a protected route returns 401 without a token
- [ ] Protect a React or Vue frontend with OIDC — complete login and display the user's name
- [ ] Create a custom login theme — replace the Keycloak logo with your own
- [ ] Configure session and token lifespan in the admin console
- [ ] Enable HTTPS locally using a self-signed cert or `mkcert`
- [ ] Review and fix CORS errors end-to-end

---

## 11. Key Takeaways

- Keep access tokens short-lived; use refresh tokens to maintain sessions
- Public clients (SPAs, mobile) must use PKCE — Keycloak enforces this automatically
- Spring Boot only needs the `issuer-uri` to validate tokens; no Keycloak adapter library required in modern versions
- Custom themes are FreeMarker templates — start by copying the default and changing CSS before touching HTML
- Always test login in an incognito window to avoid cached session confusion during development

---

## 12. Recommended Reading

- [Spring Security OAuth 2.0 Resource Server docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Keycloak — securing applications guide](https://www.keycloak.org/docs/latest/securing_apps/)
- [react-oidc-context on GitHub](https://github.com/authts/react-oidc-context)
- [Keycloak server administration — themes](https://www.keycloak.org/docs/latest/server_admin/#_themes)

---

_Previous: [Phase 1 — Foundations](./phase-1-foundations.md)_  
_Next: Phase 3 — Advanced: Microservices, Scaling & DevOps (coming soon)_
