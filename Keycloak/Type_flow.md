## Differences between flows in Keycloak

In **Keycloak**, these are different **OAuth 2.0 / OpenID Connect (OIDC) grant types (flows)**, each designed for a **specific use case**.

---

## 1️⃣ Standard Flow

👉 **Authorization Code Flow (OIDC)**

### When to use

- Web applications with a **backend** (Spring Boot, Django, Node.js, etc.)
- **Recommended by default**

### Flow

```
Browser → Keycloak (login)
Keycloak → redirect with authorization code
Backend → exchange code for access token
```

### Characteristics

- Uses an **authorization code** → secure
- Supports **refresh tokens**
- Supports **PKCE**

### Examples

- Spring Boot + Thymeleaf
- React + Backend API

✅ **Recommended for most web applications**

---

## 2️⃣ Direct Access Grants

👉 **Resource Owner Password Credentials Grant**

### When to use

- The application **already has the user’s username and password**
- No redirect to Keycloak login page

### Flow

```
Application → Keycloak
(username + password)
→ access token
```

### Characteristics

- The app **knows the user’s password ❌**
- Not recommended in modern OAuth 2.0
- High security risk

⚠️ **Use only for legacy systems**

---

## 3️⃣ Implicit Flow

👉 **Implicit Grant (Deprecated)**

### When to use

- Old SPA applications without a backend

### Flow

```
Browser → Keycloak
→ access token returned directly in URL
```

### Characteristics

- Access token exposed in the URL ❌
- No refresh token
- Deprecated in OIDC

❌ **Do NOT use**
👉 Use **Authorization Code + PKCE** instead

---

## 4️⃣ Service Accounts Roles

👉 **Client Credentials Grant**

### When to use

- **Machine-to-machine** communication
- No end user involved

### Flow

```
Service A → Keycloak
(client_id + client_secret)
→ access token
```

### Characteristics

- Token represents the **client**, not a user
- Roles are assigned directly to the client
- No user context

### Examples

- Microservices calling each other
- Background jobs, cron tasks

✅ **Very common in microservice architectures**

---

## 5️⃣ Standard Token Exchange

👉 **OAuth 2.0 Token Exchange**

### When to use

- Exchange one token for another
- Delegation between services or across realms

### Example

```
Frontend token
→ exchange for backend token
→ backend calls internal services
```

### Characteristics

- No need for the user to log in again
- Fine-grained delegation
- Implemented as a Keycloak extension

---

## 6️⃣ OAuth 2.0 Device Authorization Grant

👉 **Device Flow**

### When to use

- Devices **without a browser**
- Limited input capability

### Flow

```
TV / IoT device → device code
User → login on phone or laptop
Device → poll for access token
```

### Examples

- Smart TVs
- Game consoles
- IoT devices

---

## 7️⃣ OIDC CIBA Grant

👉 **Client-Initiated Backchannel Authentication**

### When to use

- Authentication **without browser interaction**
- Out-of-band authentication (push, OTP, mobile app)

### Flow

```
Backend → Keycloak
Keycloak → sends auth request to user device
User approves
Backend → retrieves access token
```

### Characteristics

- Very high security
- Common in banking and fintech

---

## Quick comparison

| Flow            | User | Redirect | Security | Typical Use        |
| --------------- | ---- | -------- | -------- | ------------------ |
| Standard Flow   | ✅   | ✅       | ⭐⭐⭐⭐ | Web apps           |
| Direct Access   | ✅   | ❌       | ⭐       | Legacy apps        |
| Implicit        | ✅   | ✅       | ❌       | Deprecated         |
| Service Account | ❌   | ❌       | ⭐⭐⭐⭐ | Service-to-service |
| Token Exchange  | ⚠️   | ❌       | ⭐⭐⭐   | Delegation         |
| Device Flow     | ✅   | ❌       | ⭐⭐⭐   | TV / IoT           |
| CIBA            | ✅   | ❌       | ⭐⭐⭐⭐ | Banking / Mobile   |

---

## Practical recommendations

- **Web apps** → Standard Flow + PKCE
- **SPA** → Authorization Code + PKCE
- **Microservices** → Service Accounts
- **Avoid** → Implicit Flow, Direct Access Grants (unless legacy)
