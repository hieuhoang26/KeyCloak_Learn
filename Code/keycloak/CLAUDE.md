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

Spring Boot 3.5.14 / Java 17 app that acts as an OAuth2 resource server and manages Keycloak users via the Keycloak Admin Client.

**Key layers:**

- `service/IdentityProvider` — interface with full CRUD: `createUser`, `getUserById`, `getAllUsers`, `updateUser`, `deleteUser`
- `service/imp/KeyCloakService` — implements `IdentityProvider`; builds its own `Keycloak` admin client via `@PostConstruct` using RESTEasy (`ResteasyClientBuilderImpl`) with connection pool and timeout settings from `KeycloakProperties`
- `config/KeycloakConfig` — declares a separate `Keycloak` Spring bean (used independently from the one in `KeyCloakService`); bound via `@EnableConfigurationProperties(KeycloakProperties.class)`
- `config/KeycloakProperties` — `@ConfigurationProperties(prefix = "keycloak")` binding `server-url`, `realm`, `client-id`, `client-secret`, `endpoint`, `connection-pool-size`, `connect-timeout-seconds`, `read-timeout-seconds`
- `controller/UserController` — REST CRUD at `/api/v1/users` (GET all, GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}`)
- `dto/UserDTO` — request body: `username`, `email`, `firstName`, `lastName`, `password`, `enabled`
- `dto/UserResponse` — response body: same fields minus `password`, plus `id`

**Note:** `KeycloakProperties` has both `serverUrl` (used by `KeycloakConfig` bean) and `endpoint` (used directly by `KeyCloakService`). These should map to the same Keycloak URL; keep them in sync or consolidate.

**Infrastructure (docker-compose):**
- Keycloak 24.0 on port 8080, backed by PostgreSQL 16
- Both run on the `keycloak_net` bridge network; the Spring Boot app connects to Keycloak externally on port 8080

**Active dependencies:** `keycloak-admin-client:24.0.5`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-security`, `spring-boot-starter-web`, Lombok.  
**Commented-out (not yet active):** `spring-boot-starter-data-jpa`, `liquibase-core` — uncomment in `pom.xml` when database access from the Spring app is needed.

## Environment

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8000` | Spring Boot listen port |
| `KEYCLOAK_SERVER_URL` | `http://localhost:8080` | Keycloak base URL (`keycloak.server-url`) |
| `KEYCLOAK_REALM` | `demo` | Realm name |
| `KEYCLOAK_CLIENT_ID` | `spring-client` | Admin client ID |
| `KEYCLOAK_CLIENT_SECRET` | `change-me` | Admin client secret |

Keycloak credentials (docker-compose only): admin/admin, DB user keycloak/keycloak.
