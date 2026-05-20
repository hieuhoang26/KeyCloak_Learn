# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./mvnw clean package          # Build JAR
./mvnw spring-boot:run        # Run locally (port 8000)
./mvnw test                   # Run all tests
./mvnw test -Dtest=ClassName  # Run single test class
```

Start the Keycloak server and its PostgreSQL backend:
```bash
docker-compose up -d
```

Keycloak admin UI is at `http://localhost:8080` (admin/admin). The Spring Boot app runs at `http://localhost:8000/api/v1`.

## Architecture

Spring Boot 3.5 / Java 17 OAuth2 resource server that delegates authentication to a Keycloak instance.

**Key layers:**

- `service/IdentityProvider` — interface defining identity-provider operations (currently empty; this is where new capabilities should be declared)
- `service/imp/KeyCloakService` — implements `IdentityProvider` against Keycloak's APIs
- `controller/` — REST controllers (not yet populated); all endpoints should be under the `/api/v1` context path configured in `application.yaml`

**Infrastructure (docker-compose):**
- Keycloak 24.0 on port 8080, backed by PostgreSQL 16
- Both run on the `keycloak_net` bridge network; the Spring Boot app connects to Keycloak externally on port 8080

**Active dependencies:** `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-security`, `spring-boot-starter-web`, Lombok.  
**Commented-out (not yet active):** `spring-boot-starter-data-jpa`, `liquibase-core` — uncomment these when database access from the Spring app is needed.

## Environment

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8000` | Spring Boot listen port |

Keycloak credentials (docker-compose only): admin/admin, DB user keycloak/keycloak.
