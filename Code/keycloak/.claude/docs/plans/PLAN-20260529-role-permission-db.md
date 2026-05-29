# Plan: Complete Spring Boot + Spring Security + PostgreSQL with User & Role Tables

**Date**: 2026-05-29  
**Status**: DONE  
**Completed**: 2026-05-29  
**Author**: Claude Code  

---

## Revision History
| Version | Date       | Changed By  | Summary                                          |
|---------|------------|-------------|--------------------------------------------------|
| v1.0    | 2026-05-29 | Claude Code | Initial plan                                     |
| v1.1    | 2026-05-29 | Claude Code | App DB runs on local PostgreSQL, not in Docker   |

---

## 1. Requirement Summary

The Keycloak integration is complete (JWT validation, admin-client CRUD, custom filter chain). The next step is to complete the local PostgreSQL layer by adding a `roles` table, a `user_roles` junction table, wiring those roles into Spring Security as authorities, and exposing role-management endpoints — so the app has its own RBAC layer independent of (but complementary to) Keycloak realm roles.

---

## 2. Scope

### In Scope
- `Role` JPA entity + `roles` table (Liquibase)
- `user_roles` junction table (many-to-many `User ↔ Role`)
- `RoleRepository` and `UserRoleRepository`
- `RoleService` + `RoleServiceImpl` (CRUD + assign/revoke)
- `RoleController` at `/api/v1/roles`
- Extend `UserController` with `/users/{id}/roles` endpoints
- Update `UserDetailsAdapter` to expose roles from DB as Spring Security `GrantedAuthority`
- Enable `@PreAuthorize` on controller methods using those roles
- Auto-assign `ROLE_USER` to every new user in `UserServiceImpl`
- New Liquibase changesets; update `baseline-master.xml`
- Unit tests for `RoleServiceImpl`, `UserDetailsAdapter`

### Out of Scope
- Syncing local roles back to Keycloak realm roles (kept local-only)
- Permission/resource-action table (can be added later)
- Frontend / UI
- Refresh-token handling
- Role hierarchy (`RoleHierarchy` bean)

---

## 3. Technical Design

### 3.1 App Database — Local PostgreSQL

<!-- REVISED v1.1: app DB runs locally, not in Docker -->

The Spring Boot app connects to a **local PostgreSQL** instance (separate from Keycloak's Docker Postgres). No docker-compose changes are needed.

**Prerequisites (one-time manual setup):**
```sql
-- run as superuser in local psql
CREATE DATABASE app_db;
CREATE USER postgres WITH PASSWORD '12345678';
GRANT ALL PRIVILEGES ON DATABASE app_db TO postgres;
```

**`application.yaml` datasource defaults** (already correct, no change needed):
```yaml
spring.datasource:
  url:      jdbc:postgresql://localhost:5432/app_db
  username: postgres
  password: 12345678
```

Liquibase will create all tables on first startup.

### 3.2 Data Model

#### New table: `roles`
| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID PK | `gen_random_uuid()` |
| `name` | VARCHAR(100) UNIQUE NOT NULL | e.g. `ROLE_ADMIN`, `ROLE_USER` |
| `description` | VARCHAR(255) | |
| `date_created` | TIMESTAMPTZ NOT NULL | |
| `created_by` | VARCHAR(255) NOT NULL | |
| `last_updated` | TIMESTAMPTZ NOT NULL | |
| `updated_by` | VARCHAR(255) NOT NULL | |

#### New table: `user_roles`
| Column | Type | Notes |
|--------|------|-------|
| `user_id` | UUID FK → `users.id` | composite PK |
| `role_id` | UUID FK → `roles.id` | composite PK |
| `assigned_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |
| `assigned_by` | VARCHAR(255) NOT NULL | |

#### Update: `User` entity
- Add `@ManyToMany(fetch = EAGER)` → `Role` through `user_roles`

### 3.3 Components to Create

| Component | Type | Package | Responsibility |
|-----------|------|---------|----------------|
| `Role` | Entity | `com.ex.keycloak.domain` | Maps to `roles` table, extends `BaseEntity` |
| `RoleRepository` | Repository | `com.ex.keycloak.repository` | JPA CRUD, `findByName()` |
| `RoleService` | Interface | `com.ex.keycloak.service` | `create`, `findAll`, `findByName`, `assignToUser`, `revokeFromUser` |
| `RoleServiceImpl` | Service | `com.ex.keycloak.service.imp` | Implements `RoleService` |
| `RoleController` | Controller | `com.ex.keycloak.controller` | REST endpoints for role CRUD |
| `RoleDTO` | DTO | `com.ex.keycloak.dto` | Request body for role creation |
| `RoleResponse` | DTO | `com.ex.keycloak.dto` | Response body |
| `DataInitializer` | Component | `com.ex.keycloak.config` | Seeds default `ROLE_USER`, `ROLE_ADMIN` on startup |

### 3.4 Components to Modify

| Component | Location | Change |
|-----------|----------|--------|
| `User` | `domain/User.java` | Add `@ManyToMany` `roles` field |
| `UserDetailsAdapter` | `security/` | Load roles from `user.getRoles()` as `SimpleGrantedAuthority` |
| `UserServiceImpl` | `service/imp/` | Auto-assign `ROLE_USER` when creating a user |
| `WebSecurityConfig` | `config/` | Add `@EnableMethodSecurity` |
| `UserController` | `controller/` | Add `POST /users/{id}/roles/{roleId}`, `DELETE /users/{id}/roles/{roleId}`, `GET /users/{id}/roles` |
| `UserResponse` | `dto/` | Add `List<String> roles` |
| `baseline-master.xml` | `resources/db/changelog/` | Include new changesets |
| `application.yaml` | `resources/` | Update default `DB_URL` to use `app_db` database name |

### 3.5 API Contract

```
# Role CRUD
POST   /api/v1/roles              body: { "name": "ROLE_MANAGER", "description": "..." }
GET    /api/v1/roles              → [{ "id", "name", "description" }]
GET    /api/v1/roles/{id}         → { "id", "name", "description" }
DELETE /api/v1/roles/{id}         → 204

# User-Role assignment
POST   /api/v1/users/{id}/roles/{roleId}   → 200 (assign)
DELETE /api/v1/users/{id}/roles/{roleId}   → 204 (revoke)
GET    /api/v1/users/{id}/roles            → ["ROLE_USER", "ROLE_ADMIN"]
```

**Security:**
- `POST/DELETE /roles` → requires `ROLE_ADMIN`
- `GET /roles` → requires authentication
- `POST/DELETE /users/{id}/roles/*` → requires `ROLE_ADMIN`

### 3.6 Key Decisions

- **Decision**: Fetch roles `EAGER` on `User`  
  **Reason**: `UserDetailsAdapter` is constructed inside the security filter (outside a transaction scope), so lazy loading would throw `LazyInitializationException`.  
  **Alternatives**: keep `LAZY` and fetch roles in a separate query inside `KeycloakAuthenticationProvider` — adds one query but cleaner. Choose EAGER for simplicity given small role count.

- **Decision**: Store role names with `ROLE_` prefix in DB  
  **Reason**: Spring Security's `hasRole("ADMIN")` strips the prefix; `hasAuthority("ROLE_ADMIN")` does not. Consistent prefix avoids confusion.  
  **Alternatives**: store `ADMIN` and add prefix at `GrantedAuthority` construction time.

- **Decision**: Auto-assign `ROLE_USER` in `UserServiceImpl.createUser()`  
  **Reason**: Every user must have at least one role for security expressions to work correctly.

---

## 4. Implementation Steps

- [x] **Step 1**: Create local `app_db` PostgreSQL database (manual prerequisite — see §3.1); update `application.yaml` default `DB_URL` to use `app_db`

- [x] **Step 2**: Create Liquibase changeset `20260529-create-roles.sql` — `roles` table

- [x] **Step 3**: Create Liquibase changeset `20260529-create-user-roles.sql` — `user_roles` junction table

- [x] **Step 4**: Register both changesets in `baseline-master.xml` (also added `20260529-seed-default-roles.sql`)

- [x] **Step 5**: Create `Role` entity in `com.ex.keycloak.domain` (extends `BaseEntity`)

- [x] **Step 6**: Add `@ManyToMany(fetch = EAGER)` `roles` field to `User` entity

- [x] **Step 7**: Create `RoleRepository` extending `JpaRepository<Role, UUID>` with `findByName(String name)`

- [x] **Step 8**: Create `RoleService` interface and `RoleServiceImpl` (also created `ResourceNotFoundException`)

- [x] **Step 9**: Seeded `ROLE_USER` and `ROLE_ADMIN` via Liquibase changeset `20260529-seed-default-roles.sql`

- [x] **Step 10**: Update `UserServiceImpl.createUser()` to auto-assign `ROLE_USER` after persisting the user

- [x] **Step 11**: Update `UserDetailsAdapter` to map `user.getRoles()` → `SimpleGrantedAuthority`

- [x] **Step 12**: Add `@EnableMethodSecurity` to `WebSecurityConfig`

- [x] **Step 13**: Create `RoleDTO` and `RoleResponse` DTOs

- [x] **Step 14**: Create `RoleController` with CRUD endpoints + `@PreAuthorize` guards

- [x] **Step 15**: Add `/users/{id}/roles` endpoints to `UserController`

- [x] **Step 16**: Update `UserResponse` to include `List<String> roles`

- [ ] **Step 17**: Write unit tests for `RoleServiceImpl` — skipped per user request

- [ ] **Step 18**: Write unit test for `UserDetailsAdapter` — skipped per user request

---

[//]: # (## 5. Testing Strategy)

[//]: # ()
[//]: # (**Unit tests** &#40;`src/test/.../`&#41;)

[//]: # (- `RoleServiceImplTest` — mock `RoleRepository`, `UserRepository`; test create duplicate name throws, assign non-existent user throws, revoke removes correctly)

[//]: # (- `UserDetailsAdapterTest` — construct with a `User` that has 2 roles; assert `getAuthorities&#40;&#41;` returns both `SimpleGrantedAuthority`s)

[//]: # (- `KeycloakAuthenticationProviderTest` — already exists; extend to cover a user with roles)

[//]: # ()
[//]: # (**Integration tests** &#40;optional, deferred&#41;)

[//]: # (- Against H2 in-memory &#40;schema via Liquibase test config&#41;)

[//]: # ()
[//]: # (**Manual smoke test** &#40;after docker-compose up&#41;:)

[//]: # (1. Create user → verify `ROLE_USER` auto-assigned in DB)

[//]: # (2. Assign `ROLE_ADMIN` via `POST /users/{id}/roles/{adminRoleId}`)

[//]: # (3. Call a `ROLE_ADMIN`-protected endpoint with that user's JWT → expect 200)

[//]: # (4. Call with `ROLE_USER`-only token → expect 403)

---

## 6. Risks & Open Questions

- **Risk**: Local PostgreSQL not installed or `app_db` not created before first run → **Mitigation**: §3.1 documents the one-time setup; app will fail fast with a clear datasource connection error if skipped
- **Risk**: EAGER loading `roles` in `User` increases query cost for every auth check → **Mitigation**: acceptable at this scale; add 2nd-level cache later if needed
- **Open question**: Should `DataInitializer` be idempotent (use `INSERT ... ON CONFLICT DO NOTHING`) or use a Liquibase seed changeset? → Assumption: use Liquibase seed changeset (`20260529-seed-default-roles.sql`) so it runs once and is tracked
- **Open question**: Should role CRUD endpoints require Keycloak JWT or be open during bootstrap? → Assumption: require JWT + `ROLE_ADMIN` (chicken-and-egg handled by seeding an admin user manually via Keycloak first)

---

## 7. Estimated Complexity

- [x] **Medium (2–8h)**
