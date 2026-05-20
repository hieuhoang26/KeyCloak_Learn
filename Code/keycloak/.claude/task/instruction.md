IdentityProvider — Interface

Purpose

A contract defining user lifecycle operations against an external identity provider. It abstracts away which provider
(Keycloak, Auth0, etc.) is used, letting the rest of the app depend on this interface instead of a concrete
implementation.

Class Declaration

- interface — pure contract, no state or logic
- Declares 6 operations covering the full user account lifecycle

Methods (the contract)

┌────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────┐
│                     Method                     │                        What it defines                         │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ createUser(UserDTO)                            │ Create a user without a password (e.g., invite flow)           │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ createUserWithPassword(UserDTO, String,        │ Create with a password; hasTempPassword flag triggers forced   │
│ boolean)                                       │ reset                                                          │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ findByEmailAndUsername(String, String)         │ Look up an existing user; returns Optional<UUID> (empty = not  │
│                                                │ found)                                                         │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ resetPassword(String, String)                  │ Set a new password for a user by their provider ID             │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ enableUser(String) / disableUser(String)       │ Toggle account access                                          │
├────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────┤
│ deleteUser(String)                             │ Permanently remove the account                                 │
└────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────┘

Design Notes

- The deleteUser method has a Javadoc comment (the only one) — signals it's the most dangerous/irreversible operation
  and was explicitly documented for that reason.
- Returns String (Keycloak user ID) from create methods — callers store this ID for future operations.

  ---
KeycloakService — Implementation

Purpose

The concrete Keycloak implementation of IdentityProvider. It manages a long-lived Keycloak admin client and delegates
each operation to the Keycloak Admin REST API via the keycloak-admin-client library.

Class Declaration

- @Service — Spring-managed bean, singleton scope
- @Slf4j — Lombok-injected log field
- @RequiredArgsConstructor — Lombok injects IamConfig via constructor
- implements IdentityProvider — fulfills the interface contract

Fields / State

┌───────────┬───────────┬────────────────────────────────────────────────────────────┐
│   Field   │   Type    │                          Purpose                           │
├───────────┼───────────┼────────────────────────────────────────────────────────────┤
│ iamConfig │ IamConfig │ Config values: endpoint, realm, clientId, secret, timeouts │
├───────────┼───────────┼────────────────────────────────────────────────────────────┤
│ keycloak  │ Keycloak  │ Long-lived admin client (initialized once at startup)      │
└───────────┴───────────┴────────────────────────────────────────────────────────────┘

Lifecycle Methods

@PostConstruct keycloakAdminClient() — Runs once after Spring wires the bean. Builds the Keycloak admin client using
CLIENT_CREDENTIALS grant type (machine-to-machine auth). Configures connection pool and timeouts from IamConfig.

@PreDestroy closeKeycloak() — Cleanly closes the HTTP client pool when the app shuts down.

Core Method Breakdown

createUser / createUserWithPassword
Both delegate to createUserInKeycloak(). The "with password" variant additionally sets emailVerified=true and attaches
a CredentialRepresentation. If hasTempPassword=true, it adds UPDATE_PASSWORD to required actions — Keycloak forces
the user to change their password on first login.

createUserInKeycloak() (private)
This is the core HTTP call. Key logic:
- Calls usersResource.create(kcUser) — returns a JAX-RS Response
- 409 Conflict → throws SmaileRuntimeException (email/username already exists)
- Non-201 → reads the error body and throws with details
- 201 Created → extracts the new user's UUID from the Location header (e.g., .../users/{uuid}) by slicing from the
  last / — a bit fragile but standard for Keycloak's Admin API

findByEmailAndUsername
Searches by email first (exact match), falls back to username. Returns the first match's UUID. Any exception is
wrapped — this prevents leaking Keycloak internals upstream.

enableUser / disableUser
Both follow the same pattern: fetch → check current state (no-op if already in target state) → mutate the
UserRepresentation → call user.update(). disableUser additionally calls user.logout() to invalidate active sessions
immediately.

deleteUser
Fetches the UserResource, checks for null (graceful no-op if already gone), then calls user.remove().

Design Patterns

- Strategy / Adapter: KeycloakService adapts the Keycloak Admin API to the IdentityProvider interface. You could swap
  in an Auth0Service without changing callers.
- Facade: Hides the complexity of JAX-RS Response handling, UserRepresentation construction, and credential setup
  behind clean single-purpose methods.
- Template pattern (informal): enableUser/disableUser share identical fetch-check-mutate-update structure.

Relationships

- Depends on: IamConfig, UserDTO, SmaileRuntimeException, Keycloak Admin Client library, @LogExecution AOP aspect
- Used by: Any service that needs to manage user accounts (registration, onboarding, deactivation flows)
- Lives in: Service layer — sits between business logic and the external Keycloak system

One thing worth noting

The Location header extraction at line 200–202 is slightly brittle — it trims a trailing ] because
response.getHeaders().get("Location") returns a List and .toString() wraps it in [...]. This works but relies on the
list always having exactly one element.