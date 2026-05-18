# Phase 3: First Hands-On (Local Docker)

> **Goal:** Run oauth2-proxy locally with Docker, protect a real (but simple) backend, and observe the full login flow end-to-end.

---

## Prerequisites

- Docker and Docker Compose installed
- A Google or GitHub OAuth app (instructions below)
- Familiarity with Phases 1 and 2

---

## Step 1: Create an OAuth2 app with Google

Google is the easiest provider for local testing.

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth client ID**
3. Application type: **Web application**
4. Authorized redirect URIs: `http://localhost:4180/oauth2/callback`
5. Save your **Client ID** and **Client Secret**

> **Using GitHub instead?** Go to **GitHub → Settings → Developer Settings → OAuth Apps → New OAuth App**. Set the callback URL to `http://localhost:4180/oauth2/callback`.

---

## Step 2: Generate a cookie secret

oauth2-proxy encrypts session cookies. You need a random 32-byte base64 secret:

```bash
openssl rand -base64 32
# Example output: kY8gN3x+...7s= (yours will be different)
```

Save this value — you'll use it in the config.

---

## Step 3: Create the Docker Compose setup

Create a directory and the following files:

```
oauth2-proxy-demo/
├── docker-compose.yml
└── nginx/
    └── default.conf
```

### `docker-compose.yml`

```yaml
version: "3.8"

services:

  # The "backend" — a simple nginx serving a static page
  backend:
    image: nginx:alpine
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    # NOT exposed to host — only reachable by oauth2-proxy on the internal network
    networks:
      - internal

  # oauth2-proxy in front of everything
  oauth2-proxy:
    image: quay.io/oauth2-proxy/oauth2-proxy:latest
    command:
      - --provider=google
      - --email-domain=*                        # allow any email (for testing)
      - --upstream=http://backend:80            # forward to backend
      - --http-address=0.0.0.0:4180            # listen on this address
      - --redirect-url=http://localhost:4180/oauth2/callback
      - --client-id=${GOOGLE_CLIENT_ID}
      - --client-secret=${GOOGLE_CLIENT_SECRET}
      - --cookie-secret=${COOKIE_SECRET}
      - --cookie-secure=false                  # allow http in local dev
      - --scope=openid email profile
    environment:
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      COOKIE_SECRET: ${COOKIE_SECRET}
    ports:
      - "4180:4180"                             # only oauth2-proxy is exposed
    networks:
      - internal
    depends_on:
      - backend

networks:
  internal:
    driver: bridge
```

### `nginx/default.conf`

```nginx
server {
    listen 80;

    location / {
        default_type text/html;
        return 200 '
            <html>
            <body>
                <h1>Hello from the protected backend!</h1>
                <p>You are authenticated.</p>
                <p>User: $http_x_forwarded_email</p>
            </body>
            </html>
        ';
    }
}
```

This NGINX server reads and displays the `X-Forwarded-Email` header that oauth2-proxy injects — confirming the full flow works.

### `.env` file

Create a `.env` file in the same directory (do not commit this):

```
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
COOKIE_SECRET=your-32-byte-base64-secret-here
```

---

## Step 4: Run it

```bash
cd oauth2-proxy-demo
docker compose up
```

Open your browser to `http://localhost:4180`.

**What you should see:**

1. Browser hits `http://localhost:4180`
2. oauth2-proxy sees no session cookie → redirects to Google login
3. You log in with your Google account
4. Google redirects back to `http://localhost:4180/oauth2/callback?code=...`
5. oauth2-proxy exchanges the code for tokens (you won't see this)
6. oauth2-proxy sets the `_oauth2_proxy` cookie and redirects to `/`
7. NGINX backend renders the page showing your email address

---

## Step 5: Observe what's happening

### Inspect the cookie

Open browser DevTools → Application → Cookies → `localhost`.

You'll see `_oauth2_proxy`. It's encrypted, but you can note:
- It's an `HttpOnly` cookie (not readable by JavaScript)
- It has an expiry time
- The value is a base64-encoded encrypted blob

### Inspect the request headers

In the NGINX container logs, or by changing the backend to echo all headers:

```bash
# Temporarily replace the nginx backend with an echo server to inspect headers
# Change in docker-compose.yml:
  backend:
    image: mendhak/http-https-echo:latest
    networks:
      - internal
```

Now visit `http://localhost:4180` after login. The echo server returns a JSON response showing all request headers:

```json
{
  "headers": {
    "x-forwarded-email": "yourname@gmail.com",
    "x-forwarded-user": "1234567890",
    "x-forwarded-preferred-username": "yourname",
    "x-real-ip": "172.18.0.3",
    "x-forwarded-for": "172.18.0.1"
  }
}
```

This is exactly what your Spring Boot service will receive in Phase 5.

### Force a logout

```
http://localhost:4180/oauth2/sign_out
```

This clears the session cookie. The next request will trigger the login flow again.

---

## Step 6: Understand the routes oauth2-proxy exposes

oauth2-proxy adds these special paths automatically:

| Path | Purpose |
|---|---|
| `/oauth2/sign_in` | Renders the login page (or auto-redirects) |
| `/oauth2/sign_out` | Clears the session and logs out |
| `/oauth2/callback` | Handles the OAuth redirect from the provider |
| `/oauth2/auth` | Auth-only endpoint (used by NGINX `auth_request`) |
| `/oauth2/userinfo` | Returns current user info as JSON |

All other paths are proxied to your `--upstream` backend if authenticated.

---

## Common issues and fixes

### "redirect_uri_mismatch" error from Google

Your `--redirect-url` must exactly match what's registered in Google Cloud Console.
Check for trailing slashes, `http` vs `https`, and port numbers.

### Cookie not being set

If you see a redirect loop:
- Make sure `--cookie-secure=false` is set for `http://localhost`
- Check that `--cookie-secret` is exactly 16, 24, or 32 bytes when decoded from base64

### Backend is returning 502

Check that the `backend` service name in `--upstream=http://backend:80` matches the service name in `docker-compose.yml`. Docker Compose uses service names as DNS hostnames on internal networks.

---

## What you learned in this phase

- oauth2-proxy runs as a Docker container in front of your backend
- The backend is not exposed to the outside — only oauth2-proxy is
- After login, the user's identity arrives at the backend as `X-Forwarded-*` headers
- The `_oauth2_proxy` cookie holds the encrypted session
- You can sign out by hitting `/oauth2/sign_out`

---

**Previous:** [Phase 2 — Reverse Proxy + Auth Pattern](./phase-02-reverse-proxy-auth-pattern.md)
**Next:** [Phase 4 — Deep Dive into Configuration](./phase-04-deep-dive-configuration.md)
