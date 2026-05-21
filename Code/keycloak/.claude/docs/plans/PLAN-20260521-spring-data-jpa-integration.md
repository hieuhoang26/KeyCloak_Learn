# Plan: Spring Data JPA Integration

**Date**: 2026-05-21  
**Status**: DONE  
**Author**: Claude Code  
**Completed**: 2026-05-21  
**Summary of changes**:
- Modified: `pom.xml` — added postgresql (runtime) and h2 (test) drivers
- Modified: `src/main/resources/application.yaml` — added datasource + JPA blocks
- Modified: `src/main/java/com/ex/keycloak/domain/BaseEntity.java` — @MappedSuperclass, @EntityListeners, abstract + Lombok
- Modified: `src/main/java/com/ex/keycloak/domain/User.java` — @GeneratedValue, cleaned imports
- Created: `src/main/resources/20260521-create-user.sql` — users table DDL
- Created: `src/main/java/com/ex/keycloak/repository/UserRepository.java`
- Created: `src/main/java/com/ex/keycloak/config/JpaAuditingConfig.java`
- Created: `src/main/java/com/ex/keycloak/service/UserService.java`
- Created: `src/main/java/com/ex/keycloak/service/imp/UserServiceImpl.java`
- Modified: `src/main/java/com/ex/keycloak/controller/UserController.java` — wired UserService
- Skipped: Steps 12–13 (tests) per user request

---

## 1. Requirement Summary

Integrate Spring Data JPA into the existing Keycloak-backed Spring Boot application so that the app maintains a local `users` table in PostgreSQL (the same database already running in docker-compose). This shadow table stores application-level user data (status, phone, lastLogin, audit fields) keyed by the `keycloakId` returned from Keycloak on creation. The result is a hybrid architecture: Keycloak owns authentication and identity, while the local DB owns domain-specific user state.

---

## 2. Scope

### In Scope
- Uncomment / confirm `spring-boot-starter-data-jpa` and add the PostgreSQL JDBC driver in `pom.xml`
- Add `spring.datasource` and `spring.jpa` configuration blocks to `application.yaml`
- Finalize `BaseEntity` with proper JPA annotations (`@MappedSuperclass`, `@EntityListeners`)
- Finalize `User` entity with all field/column mappings
- Create `UserRepository` extending `JpaRepository<User, UUID>`
- Create `JpaAuditingConfig` enabling `@EnableJpaAuditing` and providing an `AuditorAware` bean
- Extend `KeyCloakService` (or a new `UserService`) to persist a local `User` row on creation and sync status/lastLogin changes
- Update `UserController` to return combined data (Keycloak + local DB) where relevant
- Add `application-test.yaml` with H2 in-memory datasource for integration tests
- Create a `20260521-create-user.sql` (or Flyway V1 migration) for the `users` table DDL

### Out of Scope
- Full Flyway/Liquibase migration pipeline (can be added later; 20260521-create-user.sql is sufficient for now)
- Syncing historical Keycloak users back-fill into the local table
- Role / group management tables
- Caching layer

---

## 3. Technical Design

### Components to Create

| Component | Type | Location | Responsibility |
|-----------|------|----------|----------------|
| `UserRepository` | Repository | `com.ex.keycloak.repository` | JPA CRUD for `User` entity |
| `JpaAuditingConfig` | Config | `com.ex.keycloak.config` | Enable JPA auditing + `AuditorAware` bean |
| `UserService` | Service | `com.ex.keycloak.service` | Orchestrates Keycloak ops + local DB persistence |
| `UserService` impl | Service Impl | `com.ex.keycloak.service.imp` | Implements `UserService` |
| `20260521-create-user.sql` | SQL | `src/main/resources` | DDL for `users` table (used when `ddl-auto=none`) |

### Components to Modify

| Component | Location | Change Description |
|-----------|----------|--------------------|
| `pom.xml` | root | Add `postgresql` JDBC driver dependency; confirm JPA starter is active |
| `application.yaml` | `src/main/resources` | Add `spring.datasource`, `spring.jpa` blocks; add DB env-var defaults pointing to docker-compose Postgres |
| `BaseEntity` | `com.ex.keycloak.domain` | Add `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`, Lombok annotations |
| `User` | `com.ex.keycloak.domain` | Finalize: add `@GeneratedValue`, ensure all column constraints match DDL, clean unused imports |
| `UserController` | `com.ex.keycloak.controller` | Wire `UserService`; return enriched responses that include local DB fields |
| `KeyCloakService` | `com.ex.keycloak.service.imp` | Keep pure Keycloak operations; delegate local persistence to `UserService` |

### Data Model Changes

**Table: `users`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `uuid` | PK, default `gen_random_uuid()` |
| `keycloak_id` | `varchar(255)` | NOT NULL, UNIQUE |
| `email` | `varchar(255)` | NOT NULL, UNIQUE |
| `username` | `varchar(255)` | NOT NULL, UNIQUE |
| `full_name` | `varchar(255)` | NOT NULL |
| `phone` | `varchar(50)` | nullable |
| `status` | `varchar(20)` | NOT NULL, default `'ACTIVE'` |
| `last_login` | `timestamptz` | nullable |
| `date_created` | `timestamptz` | NOT NULL |
| `created_by` | `varchar(255)` | NOT NULL |
| `last_updated` | `timestamptz` | NOT NULL |
| `updated_by` | `varchar(255)` | NOT NULL |

### API Contract

Existing endpoints remain unchanged. After integration, `POST /api/v1/users` will:
1. Create the user in Keycloak (existing behaviour)
2. Persist a `User` row in local PostgreSQL (new)
3. Return the existing `UserResponse` (no breaking change)

`GET /api/v1/users/search` may additionally return `status`, `phone`, `lastLogin` from the local DB — mark as additive/non-breaking.

### Key Decisions

- **Decision**: Reuse the existing Keycloak PostgreSQL instance (same docker-compose service) for the app's datasource rather than spinning up a second DB.  
  **Reason**: Minimises infrastructure changes; docker-compose already exposes port 5432.  
  **Alternatives considered**: Separate H2 embedded DB (rejected — not production-grade); second Postgres container (unnecessary complexity at this stage).

- **Decision**: Use `spring.jpa.hibernate.ddl-auto=none` + `20260521-create-user.sql` for schema management.  
  **Reason**: Explicit DDL gives full control and avoids Hibernate surprises; `20260521-create-user.sql` is simple enough given one table.  
  **Alternatives considered**: `ddl-auto=update` (risky in production), full Liquibase (out of scope for now).

- **Decision**: Introduce a `UserService` interface + impl on top of `KeyCloakService`.  
  **Reason**: Keeps `KeyCloakService` focused on Keycloak HTTP operations; `UserService` owns the DB + Keycloak orchestration.  
  **Alternatives considered**: Inject repository directly into `KeyCloakService` (poor separation).

- **Decision**: `AuditorAware` returns a placeholder `"system"` initially.  
  **Reason**: No authenticated principal is reliably available during user creation from an admin flow; can be upgraded to read the JWT subject later.  
  **Alternatives considered**: Parse `SecurityContextHolder` JWT — deferred, needs auth context wiring.

---

## 4. Implementation Steps

- [x] **Step 1** — `pom.xml`: confirm `spring-boot-starter-data-jpa` is uncommented; add `org.postgresql:postgresql` driver (runtime scope)
- [x] **Step 2** — `application.yaml`: add `spring.datasource` block (url/username/password from env vars defaulting to docker-compose values) and `spring.jpa` block (`ddl-auto=none`, `show-sql=true` for dev, `open-in-view=false`)
- [x] **Step 3** — `BaseEntity`: add `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`, `@Getter`/`@Setter`
- [x] **Step 4** — `User` entity: add `@GeneratedValue(strategy = GenerationType.UUID)` to `id`; clean unused imports; verify all `@Column` constraints
- [x] **Step 5** — `UserStatus`: no changes needed; already correct
- [x] **Step 6** — `20260521-create-user.sql`: write DDL for `users` table matching entity field mappings
- [x] **Step 7** — `UserRepository`: create interface extending `JpaRepository<User, UUID>`; add `findByKeycloakId(String)`, `findByEmail(String)`, `findByUsername(String)`
- [x] **Step 8** — `JpaAuditingConfig`: create `@Configuration @EnableJpaAuditing` class with `AuditorAware<String>` bean returning `"system"` (upgradeable later)
- [x] **Step 9** — `UserService` interface: define `createUser(UserDTO)`, `findByKeycloakId(String)`, `updateStatus(String keycloakId, UserStatus)`, `recordLogin(String keycloakId)`
- [x] **Step 10** — `UserServiceImpl`: implement `UserService`; inject `IdentityProvider` (Keycloak) + `UserRepository`; on `createUser` call Keycloak then persist local row
- [x] **Step 11** — `UserController`: replace direct injection of `IdentityProvider` with `UserService`; update create endpoint to go through `UserService`
- [ ] ~~**Step 12**~~ — skipped (tests deferred)
- [ ] ~~**Step 13**~~ — skipped (tests deferred)

---

## 5. Testing Strategy

**Unit tests (`UserServiceImplTest`)**
- Mock `IdentityProvider` and `UserRepository`
- Verify `createUser` calls Keycloak then saves entity
- Verify `updateStatus` updates only the `status` field
- Edge cases: Keycloak throws → no DB write (transactional rollback)

**Integration tests (`UserControllerIT`)**
- Use H2 in-memory DB (`application-test.yaml`)
- Mock Keycloak via `@MockBean IdentityProvider`
- Assert `POST /api/v1/users` returns 201 and a row exists in H2
- Assert `GET /api/v1/users/search?email=x` returns enriched response with `status`

**No mocking of the JPA layer** — repositories hit real (H2) DB in integration tests.

---

## 6. Risks & Open Questions

- **Risk**: Spring Boot auto-configures `20260521-create-user.sql` only when `ddl-auto=none` or `never` **and** `spring.sql.init.mode=always`.  
  **Mitigation**: Explicitly set `spring.sql.init.mode=always` in config.

- **Risk**: `keycloak_id` UNIQUE constraint will cause duplicate-key errors if Keycloak create fails mid-retry.  
  **Mitigation**: Wrap Keycloak call + DB save in a single `@Transactional` method; Keycloak side is idempotent by email.

- **Risk**: Existing docker-compose PostgreSQL already has a `keycloak` database — the app should connect to the same instance but may need a separate schema or database (`app`).  
  **Mitigation**: Create a second database `app` in docker-compose init SQL, or use a separate schema `app_schema` in the same database. Decide before Step 6.

- **Open question**: Should `User.fullName` be derived from Keycloak's `firstName + lastName`, or stored independently? Current `UserDTO` has `firstName`/`lastName` but `User` entity has `fullName`.  
  **Assumed answer**: Concatenate on creation (`firstName + " " + lastName`). Confirm with team.

---

## 7. Estimated Complexity

- [x] **Medium (2–8h)** — straightforward integration but requires coordinating entity design, datasource config, service layering, and tests.
