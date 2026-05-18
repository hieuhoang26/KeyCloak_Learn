# Phase 2: Reverse Proxy + Auth Pattern

> **Goal:** Understand the architectural pattern oauth2-proxy implements — before writing a single line of config.

---

## What is a reverse proxy?

A **reverse proxy** sits in front of your backend services. Clients never talk to your services directly — they talk to the proxy, and the proxy forwards requests on their behalf.

```
Without proxy:
  Client ──────────────────────────► Backend Service

With reverse proxy:
  Client ──► Reverse Proxy ──────► Backend Service
```

Common reverse proxies: NGINX, Traefik, HAProxy, Envoy.

oauth2-proxy is a **specialized reverse proxy** that adds authentication to this flow. It intercepts every request and checks: *is this user logged in?* If not, it redirects them to the login page. If yes, it forwards the request — with identity headers injected.

---

## The oauth2-proxy flow

```
                      ┌─────────────────────────────────┐
                      │          oauth2-proxy            │
  Browser ──────────► │                                  │ ──────────► Your Service
                      │  1. Check session cookie         │
                      │  2. Redirect to login if absent  │
                      │  3. Handle OAuth callback        │
                      │  4. Store session                │
                      │  5. Inject identity headers      │
                      └─────────────────────────────────┘
                                     │
                                     │ OAuth2/OIDC
                                     ▼
                         Authorization Server
                         (Google, Keycloak, GitHub…)
```

### Step by step

**Request arrives (no session):**

1. Browser requests `https://app.example.com/dashboard`
2. oauth2-proxy checks for `_oauth2_proxy` cookie — not found
3. oauth2-proxy redirects browser to `https://accounts.google.com/o/oauth2/auth?...`
4. User logs in at Google
5. Google redirects back to `https://app.example.com/oauth2/callback?code=ABC`
6. oauth2-proxy exchanges the code for tokens (server-side)
7. oauth2-proxy creates a session (cookie or Redis), stores tokens
8. oauth2-proxy redirects browser back to `/dashboard`

**Request arrives (session exists):**

1. Browser requests `/dashboard` with `_oauth2_proxy` cookie
2. oauth2-proxy validates the session
3. oauth2-proxy injects identity headers into the request
4. oauth2-proxy forwards the request to your backend
5. Your backend sees `X-Forwarded-Email: user@example.com` and responds
6. Response passes back through to the browser

---

## The headers oauth2-proxy injects

When forwarding a request to your backend, oauth2-proxy adds these headers:

```
X-Forwarded-User:      <subject claim from ID token>
X-Forwarded-Email:     user@example.com
X-Forwarded-Groups:    engineering,admin
X-Forwarded-Preferred-Username: janesmith
Authorization:         Bearer <access_token>   ← if configured
```

Your backend reads these headers to know who is making the request. **No token validation needed in your backend** — oauth2-proxy already did it.

---

## Trust boundary — the most important concept in this phase

When your backend trusts `X-Forwarded-Email`, it is trusting that **only oauth2-proxy can set that header**. If a malicious client sends a raw HTTP request directly to your backend with `X-Forwarded-Email: admin@company.com`, your backend will believe it.

This is **header spoofing** — and it is a real attack vector.

### How to prevent it

**Option 1: Network isolation (most common)**

Your backend must only accept connections from oauth2-proxy. In Kubernetes, this means:

```
Internet → Ingress → oauth2-proxy → backend (no direct internet access)
```

The backend service is not exposed externally. It only listens on the cluster network.

```yaml
# Backend service: ClusterIP only (no LoadBalancer/NodePort)
apiVersion: v1
kind: Service
metadata:
  name: my-backend
spec:
  type: ClusterIP   # ← not reachable from outside the cluster
  ports:
    - port: 8080
```

**Option 2: Strip headers at the entry point**

In NGINX, strip these headers from incoming requests before they reach oauth2-proxy:

```nginx
# Remove any client-supplied forwarded headers before proxying
proxy_set_header X-Forwarded-Email "";
proxy_set_header X-Forwarded-User "";
```

This ensures that only oauth2-proxy — not the client — can set them.

**Option 3: Validate the JWT in your backend**

Instead of trusting headers, your backend validates the `Authorization: Bearer <token>` header itself using the Authorization Server's public keys (JWKS endpoint). More secure, more complex — covered in Phase 5.

---

## What oauth2-proxy does NOT do

It's important to understand the boundary:

| oauth2-proxy does | oauth2-proxy does NOT |
|---|---|
| Authenticate the user (login flow) | Authorize (is this user allowed to do X?) |
| Manage sessions (cookies / Redis) | Manage application-level permissions |
| Inject identity headers | Enforce role-based access control |
| Refresh tokens silently | Business logic |
| Protect all routes uniformly | Per-route fine-grained access (partially) |

Fine-grained authorization (e.g. "only admins can access `/admin`") is something you implement in your backend, using the identity information oauth2-proxy provides.

---

## Session storage basics

oauth2-proxy must store the session (tokens + user info) somewhere between requests. It has two options:

### Cookie-based session (default)

- The entire session is encrypted and stored in a cookie (`_oauth2_proxy`)
- No server-side storage required
- Cookie has a size limit (~4KB) — fine for most cases
- Suitable for a single instance

### Redis-based session

- The session is stored in Redis; the cookie holds only a session ID
- No cookie size limit
- Required when running multiple oauth2-proxy instances (horizontal scaling)
- Required when you need server-side session revocation

```
Cookie session:  Browser cookie = encrypted(user_info + tokens)
Redis session:   Browser cookie = session_id  →  Redis: session_id → {user_info, tokens}
```

---

## Checklist before moving on

- [ ] You can explain what a reverse proxy is
- [ ] You can draw the oauth2-proxy request flow from memory (unauthenticated + authenticated)
- [ ] You understand why header spoofing is a risk and how to prevent it
- [ ] You know the difference between cookie and Redis sessions
- [ ] You understand that oauth2-proxy handles **authentication**, not **authorization**

---

**Previous:** [Phase 1 — OAuth2 & OIDC Foundation](./phase-01-oauth2-oidc-foundation.md)
**Next:** [Phase 3 — First Hands-On (Local Docker)](./phase-03-first-hands-on.md)
