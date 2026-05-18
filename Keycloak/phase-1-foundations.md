# Phase 1 — Foundations: IAM & Keycloak Basics

> **Timeline:** Month 1  
> **Goal:** Understand the theory, get Keycloak running locally, and complete your first login flow end-to-end.

---

## 1. IAM Fundamentals

### Authentication vs Authorization

| Concept                    | Question it answers           | Example                               |
| -------------------------- | ----------------------------- | ------------------------------------- |
| **Authentication (AuthN)** | _Who are you?_                | Login with username + password        |
| **Authorization (AuthZ)**  | _What are you allowed to do?_ | Admin can delete users; viewer cannot |

### Single Sign-On (SSO)

SSO lets a user log in once and access multiple applications without re-entering credentials. Keycloak acts as the central Identity Provider (IdP) — apps delegate authentication to it instead of managing credentials themselves.

```
User → App A → Keycloak (login once) → Token issued
User → App B → Keycloak (session exists) → Token issued immediately
```

### Identity Federation

Federation allows Keycloak to trust an external IdP (Google, GitHub, LDAP, an enterprise SAML provider) and map their users into Keycloak. Users log in with their existing corporate or social accounts; Keycloak handles the translation.

---

## 2. Core Protocols

### OAuth 2.0

OAuth 2.0 is an **authorization** framework — it defines how an application can obtain limited access to a user's resources without handling their password.

Key roles:

- **Resource Owner** — the user
- **Client** — your application
- **Authorization Server** — Keycloak
- **Resource Server** — the API being protected

### OpenID Connect (OIDC)

OIDC is an **identity layer** on top of OAuth 2.0. Where OAuth 2.0 issues an opaque access token to call APIs, OIDC additionally issues an **ID Token** (a JWT) that tells the client _who_ the user is.

```
OAuth 2.0  →  "Here is a token to call this API"
OIDC       →  "Here is a token to call this API" + "Here is who the user is"
```

Key tokens:

- **ID Token** — JWT containing user identity claims (`sub`, `email`, `name`)
- **Access Token** — used to call protected APIs; may be JWT or opaque
- **Refresh Token** — used to obtain new access tokens without re-login

### SAML 2.0

SAML is an older XML-based protocol, still common in enterprise environments. Keycloak supports it for legacy integrations. Prefer OIDC for new projects — it is simpler, JSON-based, and better suited for SPAs and mobile apps.

---

## 3. Keycloak Core Concepts

### Realm

A realm is an isolated namespace. It contains its own users, clients, roles, and settings. Think of it as a tenant.

```
Keycloak instance
├── master realm          ← admin realm, do not use for applications
├── my-app-dev realm      ← development environment
└── my-app-prod realm     ← production environment
```

> **Best practice:** Never use the `master` realm for your own applications. Create a dedicated realm per environment or per product.

### Client

A client represents an application that delegates authentication to Keycloak. Each client has a type:

| Client type    | Use case                                              | Example         |
| -------------- | ----------------------------------------------------- | --------------- |
| `public`       | Browser SPA, mobile app — cannot keep a secret        | React frontend  |
| `confidential` | Backend service that can store a secret securely      | Spring Boot API |
| `bearer-only`  | API that only validates tokens, never initiates login | Microservice    |

### Users & Groups

- **User** — a person (or service account) with credentials stored in Keycloak
- **Group** — a collection of users; roles assigned to a group propagate to all members

### Roles

Roles are permissions assigned to users or clients.

- **Realm roles** — scoped to the entire realm
- **Client roles** — scoped to a specific client
- **Composite roles** — a role that contains other roles

```
user               → realm role: "viewer"
admin              → realm role: "admin" (composite: viewer + write + delete)
service-account-x  → client role: "internal-api-access"
```

### Identity Providers (IdPs)

External identity sources Keycloak can federate with:

- Social: Google, GitHub, Facebook
- Enterprise: Microsoft AD FS, Okta, another Keycloak realm
- Standard: any SAML 2.0 or OIDC-compliant provider

### User Federation

Connects Keycloak to an existing user store — most commonly LDAP / Active Directory. Keycloak syncs or proxies users from the directory; credentials are validated against the external source.

---

## 4. Keycloak Architecture

```
Browser / Client App
        │
        ▼
   Keycloak Server          ← Authorization Server (OAuth 2.0 / OIDC / SAML)
   ┌────────────────────┐
   │  Realm              │
   │  ├─ Clients         │
   │  ├─ Users           │
   │  ├─ Roles           │
   │  └─ Identity Providers│
   └────────┬───────────┘
            │
     ┌──────┴──────┐
     │             │
  Database     User Federation
  (PostgreSQL)  (LDAP / AD)
```

Keycloak persists configuration and user data in a relational database (PostgreSQL recommended for production). By default it ships with an embedded H2 database — fine for development, not for production.

---

## 5. Action Steps

### Step 1 — Run Keycloak with Docker

```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

Admin console: http://localhost:8080 — log in with `admin / admin`.

### Step 2 — Create a Realm

1. Open the admin console
2. Click the dropdown next to **master** (top-left) → **Create realm**
3. Name it `demo`
4. Click **Create**

### Step 3 — Create a User

1. In the `demo` realm, go to **Users** → **Add user**
2. Set username: `testuser`
3. Go to the **Credentials** tab → set a password, toggle **Temporary** off
4. Click **Save**

### Step 4 — Create a Client

1. Go to **Clients** → **Create client**
2. Client type: `OpenID Connect`
3. Client ID: `demo-app`
4. Client authentication: **Off** (public client)
5. Valid redirect URIs: `http://localhost:3000/*`
6. Click **Save**

### Step 5 — Test Login (OIDC Playground)

Open this URL in your browser (replace values if needed):

```
http://localhost:8080/realms/demo/protocol/openid-connect/auth
  ?client_id=demo-app
  &response_type=code
  &redirect_uri=http://localhost:3000/callback
  &scope=openid profile email
```

Log in as `testuser`. You will be redirected with an authorization `code` in the URL. This confirms the authorization code flow is working.

---

## 6. Key Takeaways

- Keycloak is an **Authorization Server** — your apps never see user passwords
- Every app registers as a **client** in a **realm**
- OIDC builds on OAuth 2.0 and is the preferred protocol for new projects
- The ID Token identifies the user; the Access Token authorizes API calls
- Keep the `master` realm for Keycloak administration only

---

## 7. Recommended Reading

- [Keycloak documentation — getting started](https://www.keycloak.org/guides)
- [OAuth 2.0 simplified (aaronparecki.com)](https://aaronparecki.com/oauth-2-simplified/)
- [OpenID Connect specification](https://openid.net/specs/openid-connect-core-1_0.html)
- [JWT.io — inspect tokens](https://jwt.io)

---

_Next: [Phase 2 — Intermediate: Configuration & App Integration](./phase-2-intermediate.md)_
