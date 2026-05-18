# Keycloak Foundations

## What is IAM?

Keycloak is an open-source Identity and Access Management (IAM) solution that provides authentication and authorization for modern applications and services. Originally developed by Red Hat, it is now maintained by the Keycloak community.

- Who can sign in (Identity / Authentication)
- What they can do (Access Control / Authorization)

| Concept                | Meaning                                  |
| ---------------------- | ---------------------------------------- |
| Authentication (AuthN) | Verify who the user is (sign-in)         |
| Authorization (AuthZ)  | Determine what the user is allowed to do |

Keycloak = IAM Server → manages users, sign-in, permissions, SSO, tokens…

---

## Single Sign-On (SSO)

SSO = Sign in once → access multiple applications.

Example:

- Sign in to Google once → Gmail, YouTube, Drive are already signed in.

Keycloak supports SSO by default and is well-suited for microservices architectures.

---

## OAuth2 and OpenID Connect (OIDC)

### OAuth2

A framework for authorization — issues Access Tokens that grant access to resources.

### OIDC

An extension of OAuth2 that adds user identity via the ID Token.

---

## Core Concepts

1. Realm

   - A tenant or workspace that isolates users, roles, and clients.
   - Examples: dev-realm, staging-realm, production-realm.

2. Clients

   - Applications or services that delegate authentication to Keycloak (e.g., React frontend, FastAPI backend, mobile app).
   - Purpose: Obtain tokens from Keycloak after login.
   - Protocols: Typically OIDC or SAML.

3. Users

   - People or systems that authenticate.
   - Stored info: username, email, credentials, roles, attributes.
   - Roles can be realm-level or client-specific.

4. Roles

   - Realm roles apply across the whole realm.
   - Client roles apply to a specific client.
   - Purpose: Define permissions and capabilities.

5. Groups

   - Collections of users that share roles and attributes.
   - Purpose: Assign permissions at scale.

6. Identity Providers (IdPs)

   - Examples: Google, Facebook, GitHub, LDAP.
   - Purpose: Allow users to log in without creating a separate Keycloak account.

7. Client Scopes

   - Define which claims and data are included in tokens for a given client.

8. Protocol Mappers

   - Control how user attributes and other data are mapped into tokens.

9. User Federation
   - Connect external user stores ( LDAP, Active Directory, custom user stores )

### Visual structure

```
Realm
 ├── Users
 ├── Groups
 ├── Roles
 └── Clients (Applications)
```

---

## OIDC Login Flow (Authorization Code Flow)

![flow](img/keycloak_flow.png)

1. Frontend redirects user to Keycloak login page.
2. User enters username/password (or uses IdP).
3. Keycloak issues tokens (ID Token, Access Token, Refresh Token).
4. Frontend receives tokens and sends the Access Token to the backend API.
5. Backend verifies the Access Token and processes the request.

Backend does not handle user passwords — it only validates tokens for security and scalability.

## ![flow](img/keycloak_flow_2.png)

## Mini Exercise (Understand tokens quickly)

1. Open: https://jwt.io
2. Paste a sample JWT.
3. Observe:
   - `sub` = user ID
   - `preferred_username` = username
   - `realm_access.roles` = roles assigned to the user

This is the data Keycloak sends to applications after login.

---
