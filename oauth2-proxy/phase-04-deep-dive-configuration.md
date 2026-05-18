# Phase 4: Deep Dive into Configuration

> **Goal:** Read and understand oauth2-proxy configuration like an engineer. Know what every important option does, why it exists, and what breaks when it's wrong.

---

## Configuration methods

oauth2-proxy can be configured three ways (all equivalent):

```bash
# 1. CLI flags (good for Docker / Kubernetes)
oauth2-proxy --provider=google --client-id=... --upstream=http://backend

# 2. Environment variables (prefix with OAUTH2_PROXY_, uppercase, dashes → underscores)
OAUTH2_PROXY_PROVIDER=google
OAUTH2_PROXY_CLIENT_ID=...
OAUTH2_PROXY_UPSTREAM=http://backend

# 3. Config file (alpha.cfg format)
provider = "google"
client_id = "..."
upstream = "http://backend"
```

In production (Kubernetes), **environment variables** are the most common — they integrate cleanly with Secrets.

---

## Provider configuration

### `--provider`

Which OAuth2/OIDC provider to use.

```
--provider=google
--provider=github
--provider=oidc          # generic OIDC (Keycloak, Auth0, Okta, Azure AD, etc.)
--provider=azure
--provider=gitlab
```

When using `--provider=oidc`, you must also supply the issuer URL so oauth2-proxy can auto-discover the endpoints:

```
--oidc-issuer-url=https://your-keycloak.com/realms/your-realm
```

oauth2-proxy hits `{issuer-url}/.well-known/openid-configuration` and reads all endpoint URLs from it.

### `--client-id` and `--client-secret`

Your app's credentials registered with the provider.

```
--client-id=abc123.apps.googleusercontent.com
--client-secret=GOCSPX-...
```

**Never hardcode these in a Dockerfile or commit them to git.** Use environment variables or Kubernetes Secrets.

### `--scope`

What permissions to request from the provider.

```
--scope="openid email profile"
```

Always include `openid` when using OIDC. Add `offline_access` if you need Refresh Tokens.

### `--redirect-url`

The callback URL. Must match exactly what's registered with your OAuth app.

```
--redirect-url=https://app.example.com/oauth2/callback
```

In local development:
```
--redirect-url=http://localhost:4180/oauth2/callback
```

---

## Access control

### `--email-domain`

Only allow users whose email matches this domain.

```
--email-domain=yourcompany.com       # only @yourcompany.com
--email-domain=*                     # any email (use only in dev/testing)
```

Multiple domains:
```
--email-domain=company.com
--email-domain=partner.com
```

### `--authenticated-emails-file`

Allow only specific email addresses from a file:

```
--authenticated-emails-file=/etc/oauth2-proxy/emails.txt
```

```
# emails.txt
alice@example.com
bob@example.com
```

### `--allowed-group`

Allow only users who belong to a specific group (works with providers that include groups in the ID Token, like Keycloak or GitHub teams):

```
--allowed-group=engineering
```

---

## Upstream configuration

### `--upstream`

Where to forward authenticated requests.

```
--upstream=http://my-service:8080
```

Multiple upstreams are supported (path-based routing):

```
--upstream=http://service-a:8080/api/users
--upstream=http://service-b:8080/api/orders
```

### `--skip-auth-routes`

Regex patterns for paths that bypass authentication entirely:

```
--skip-auth-route="^/health$"
--skip-auth-route="^/public/"
--skip-auth-route="^/api/webhook"
```

Useful for health check endpoints that don't need auth.

### `--skip-auth-preflight`

Skip authentication for HTTP OPTIONS requests (needed for CORS preflight):

```
--skip-auth-preflight=true
```

---

## Cookie configuration

The session cookie is how oauth2-proxy tracks logged-in users across requests. Getting this right matters a lot.

### `--cookie-secret`

Used to encrypt the cookie. **Must be 16, 24, or 32 bytes** (decoded from base64).

```bash
# Generate correctly:
openssl rand -base64 32
```

If you change this secret, all existing sessions are immediately invalidated (everyone gets logged out).

### `--cookie-name`

The name of the session cookie. Default: `_oauth2_proxy`.

```
--cookie-name=_oauth2_proxy
```

Change this if you're running multiple oauth2-proxy instances for different apps on the same domain.

### `--cookie-domain`

Which domain the cookie applies to.

```
--cookie-domain=.example.com    # all subdomains of example.com
--cookie-domain=app.example.com # only this specific subdomain
```

Important for SSO across subdomains — the cookie must be set on the root domain (`.example.com`) so `api.example.com`, `admin.example.com`, etc. all share the same session.

### `--cookie-expire`

How long the session lasts before requiring re-login.

```
--cookie-expire=168h    # 7 days (default)
--cookie-expire=8h      # workday session
```

### `--cookie-refresh`

How often to refresh the session (and the underlying tokens) silently.

```
--cookie-refresh=1h     # refresh every hour
```

Should be less than the Access Token expiry time from your provider.

### `--cookie-secure`

Whether to send the cookie over HTTPS only.

```
--cookie-secure=true     # production (always)
--cookie-secure=false    # local http development only
```

Never set `false` in production.

### `--cookie-httponly`

Prevent JavaScript from reading the cookie. Default `true`. **Do not disable this.**

---

## Session storage: Cookie vs Redis

### Default: cookie storage

The entire session (user info + encrypted tokens) is stored in the cookie itself.

**Pros:** No infrastructure required, simple to set up.  
**Cons:** Cookie size limit (~4KB), no server-side revocation.

### Redis session storage

```
--session-store-type=redis
--redis-connection-url=redis://redis:6379
```

With Redis:
- Cookie holds only a session ID (small and fast)
- Tokens are stored server-side
- Sessions can be revoked server-side
- Multiple oauth2-proxy instances share sessions — required for horizontal scaling

```yaml
# docker-compose addition
redis:
  image: redis:7-alpine
  networks:
    - internal
```

```
# oauth2-proxy config
--session-store-type=redis
--redis-connection-url=redis://redis:6379
```

**Rule of thumb:** Use cookie sessions for simple single-instance setups. Switch to Redis before scaling to multiple instances.

---

## Listening address

### `--http-address`

Where oauth2-proxy listens for incoming HTTP connections.

```
--http-address=0.0.0.0:4180    # listen on all interfaces, port 4180
--http-address=127.0.0.1:4180  # listen on loopback only
```

### `--https-address` and TLS

For TLS termination at oauth2-proxy itself (less common — usually TLS is terminated at the load balancer):

```
--https-address=:443
--tls-cert-file=/etc/tls/cert.pem
--tls-key-file=/etc/tls/key.pem
```

---

## Logging and headers

### `--set-xauthrequest`

Enables the `/oauth2/auth` endpoint to return user info headers in the response. Required when using NGINX `auth_request` (Phase 7):

```
--set-xauthrequest=true
```

### `--pass-access-token`

Forward the Access Token to the upstream service as a header:

```
--pass-access-token=true
```

The backend will receive `X-Forwarded-Access-Token: eyJ...`.

### `--pass-authorization-header`

Forward the Access Token in the `Authorization: Bearer` header:

```
--pass-authorization-header=true
```

Use this when your backend validates JWTs itself (Phase 5 Option B).

---

## Full production-ready config example

```bash
oauth2-proxy \
  # Provider
  --provider=oidc \
  --oidc-issuer-url=https://keycloak.example.com/realms/myrealm \
  --client-id=${CLIENT_ID} \
  --client-secret=${CLIENT_SECRET} \
  --scope="openid email profile groups" \
  \
  # Access control
  --email-domain=example.com \
  --allowed-group=app-users \
  \
  # Upstream
  --upstream=http://backend:8080 \
  --skip-auth-route="^/health$" \
  \
  # Cookie
  --cookie-secret=${COOKIE_SECRET} \
  --cookie-domain=.example.com \
  --cookie-secure=true \
  --cookie-expire=8h \
  --cookie-refresh=1h \
  \
  # Session (Redis for production)
  --session-store-type=redis \
  --redis-connection-url=redis://redis:6379 \
  \
  # Headers
  --set-xauthrequest=true \
  --pass-access-token=true \
  \
  # Redirect
  --redirect-url=https://app.example.com/oauth2/callback \
  --http-address=0.0.0.0:4180
```

---

## Quick reference

| Flag | Required? | What it does |
|---|---|---|
| `--provider` | Yes | Which OAuth2 provider |
| `--client-id` | Yes | Your app's client ID |
| `--client-secret` | Yes | Your app's client secret |
| `--cookie-secret` | Yes | Encrypts sessions (32 bytes) |
| `--redirect-url` | Yes | Callback URL (must match provider) |
| `--upstream` | Yes | Where to forward requests |
| `--email-domain` | Recommended | Restrict access by email domain |
| `--oidc-issuer-url` | When `--provider=oidc` | OIDC discovery URL |
| `--cookie-secure` | Yes in prod | HTTPS-only cookie |
| `--session-store-type=redis` | When scaling | Use Redis for sessions |

---

**Previous:** [Phase 3 — First Hands-On](./phase-03-first-hands-on.md)
**Next:** [Phase 5 — Spring Boot Integration](./phase-05-spring-boot-integration.md)
