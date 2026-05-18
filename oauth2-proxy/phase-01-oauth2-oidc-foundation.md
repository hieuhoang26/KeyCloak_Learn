# Phase 1: OAuth2 & OIDC Foundation

> **Goal:** Understand the auth concepts that oauth2-proxy is built on. If these feel fuzzy, oauth2-proxy will feel like magic — and that's a bad sign.

---

## Why this phase matters

oauth2-proxy does not invent anything new. It implements standard OAuth2/OIDC flows on your behalf. Every config option, every cookie, every header it injects — all of it maps directly to concepts in these specs. Learn the specs first and the tool will make sense immediately.

---

## 1. OAuth2 in plain language

**OAuth2** is an authorization framework. It lets a user grant a third-party application limited access to their account without sharing their password.

The key insight: OAuth2 separates **who you are** from **what you're allowed to do**.

### The four roles

| Role                     | Description                | Example                  |
| ------------------------ | -------------------------- | ------------------------ |
| **Resource Owner**       | The user who owns the data | You                      |
| **Client**               | The app requesting access  | Your Spring Boot app     |
| **Authorization Server** | Issues tokens after login  | Keycloak, Google, GitHub |
| **Resource Server**      | Hosts the protected data   | Your API                 |

### The Authorization Code Flow (memorize this)

This is the most important flow. oauth2-proxy uses it exclusively for browser-based auth.

```
1. User visits your app
2. App redirects user → Authorization Server (with client_id, redirect_uri, scope, state)
3. User logs in at the Authorization Server
4. Authorization Server redirects back → your app (with a short-lived `code`)
5. Your app (server-side) exchanges `code` for tokens (using client_secret)
6. Authorization Server returns: Access Token + ID Token + (optional) Refresh Token
7. Your app uses the tokens
```

**Why the two-step code exchange?**
The `code` is short-lived and useless without the `client_secret`. This prevents tokens from leaking via browser history or logs, since tokens are only exchanged in a server-to-server call.

```
Browser                    oauth2-proxy              Authorization Server
   |                            |                            |
   |--- GET /protected -------->|                            |
   |<-- 302 redirect to login --|                            |
   |                            |                            |
   |--- GET /authorize ---------------------------------------->|
   |<-- 302 redirect with ?code=ABC ----------------------------|
   |                            |                            |
   |--- GET /callback?code=ABC ->|                            |
   |                            |--- POST /token (code+secret)->|
   |                            |<-- { access_token, id_token } |
   |<-- 200 OK (set cookie) ----|                            |
```

---

## 2. The three tokens

### Access Token

- Proves the client is authorized to access a resource
- Sent to the **Resource Server** (your API) in the `Authorization: Bearer <token>` header
- Short-lived (typically 5–60 minutes)
- The Resource Server validates it — it does **not** need to call the Authorization Server every time (JWTs are self-validating)
- **Your app should not trust it to know who the user is** — that's the ID Token's job

### ID Token

- Proves **who** the user is (authentication, not authorization)
- A JWT issued by the Authorization Server
- Contains **claims**: `sub` (user ID), `email`, `name`, `iat`, `exp`, etc.
- Meant to be consumed by the **Client** (your app), not sent to APIs
- oauth2-proxy reads this token and extracts claims to inject as headers (`X-Forwarded-Email`, etc.)

### Refresh Token

- Long-lived credential used to get new Access Tokens when they expire
- Never sent to Resource Servers
- Stored securely server-side (oauth2-proxy stores it in the session)
- When the Access Token expires, oauth2-proxy uses the Refresh Token silently — the user stays logged in

```
Token           Sent to              Purpose                    Lifetime
──────────────────────────────────────────────────────────────────────
Access Token    Resource Server      Authorize API calls        5–60 min
ID Token        Client (proxy/app)   Know who the user is       5–60 min
Refresh Token   Auth Server only     Get new Access Tokens      hours–days
```

---

## 3. OpenID Connect (OIDC)

**OIDC is OAuth2 + identity.** It adds a standardized way to get user information on top of OAuth2.

Key additions OIDC makes:

- Defines the **ID Token** format (always a JWT)
- Adds a standard `/.well-known/openid-configuration` discovery endpoint
- Adds the `openid` scope (required to get an ID Token)
- Standardizes claim names (`sub`, `email`, `name`, `given_name`, etc.)

When you configure oauth2-proxy with `--provider=oidc`, it hits the discovery endpoint automatically to find token URLs, JWKS endpoints, and supported scopes.

```
GET https://accounts.google.com/.well-known/openid-configuration

→ Returns JSON with:
  - authorization_endpoint
  - token_endpoint
  - userinfo_endpoint
  - jwks_uri           ← used to validate ID Token signatures
  - scopes_supported
```

---

## 4. Scopes and Claims

### Scopes

Scopes are what the client **requests** from the Authorization Server.

| Scope            | What it unlocks                      |
| ---------------- | ------------------------------------ |
| `openid`         | Required for OIDC. Enables ID Token. |
| `email`          | User's email address in the ID Token |
| `profile`        | Name, picture, locale                |
| `offline_access` | Refresh Token                        |

In oauth2-proxy, you configure scopes via `--scope="openid email profile"`.

### Claims

Claims are the **data inside a token** (specifically the ID Token or UserInfo response).

```json
{
  "sub": "1234567890",
  "email": "user@example.com",
  "email_verified": true,
  "name": "Jane Smith",
  "given_name": "Jane",
  "iat": 1716000000,
  "exp": 1716003600,
  "iss": "https://accounts.google.com",
  "aud": "your-client-id"
}
```

oauth2-proxy reads these claims and forwards them to your backend as HTTP headers:

```
X-Forwarded-User:  1234567890
X-Forwarded-Email: user@example.com
X-Forwarded-Groups: engineering,admin
```

---

## 5. Key question to answer before moving on

Ask yourself — can you answer these clearly?

1. What is the difference between an Access Token and an ID Token?
2. Why is there a two-step code exchange instead of returning the token directly?
3. What does the `openid` scope unlock?
4. Who validates the Access Token — the client or the resource server?
5. What is the purpose of the Refresh Token and who stores it?

If any of these are unclear, re-read that section before moving to Phase 2.

---

## Reference

- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [jwt.io](https://jwt.io) — decode and inspect JWTs in your browser

---

**Next:** [Phase 2 — Reverse Proxy + Auth Pattern](./phase-02-reverse-proxy-auth-pattern.md)
