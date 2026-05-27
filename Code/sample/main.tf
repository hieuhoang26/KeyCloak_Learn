# =============================================================================
# Keycloak Realm — Spring Boot + JWT + Role-Based Auth + Client Frontend
# =============================================================================


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  1. REALM                                                                ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

resource "keycloak_realm" "app" {
  realm        = var.realm_name
  enabled      = true
  display_name = var.realm_display_name

  # ── Login Settings ──────────────────────────────────────────────────────
  login_with_email_allowed  = true
  duplicate_emails_allowed  = false
  reset_password_allowed    = true
  remember_me               = true
  registration_allowed      = false
  edit_username_allowed     = false

  # ── Token Lifespans ────────────────────────────────────────────────────
  access_token_lifespan           = "${var.access_token_lifespan}s"
  sso_session_idle_timeout        = "${var.refresh_token_lifespan}s"
  sso_session_max_lifespan        = "${var.sso_session_max_lifespan}s"
  offline_session_idle_timeout    = "2592000s" # 30 days
  offline_session_max_lifespan    = "5184000s" # 60 days

  # ── Security ───────────────────────────────────────────────────────────
  ssl_required    = "external"
  password_policy = "length(8) and upperCase(1) and lowerCase(1) and digits(1) and specialCharacters(1)"

  security_defenses {
    brute_force_detection {
      permanent_lockout                = false
      max_login_failures               = 5
      wait_increment_seconds           = 60
      quick_login_check_milli_seconds  = 1000
      minimum_quick_login_wait_seconds = 60
      max_failure_wait_seconds         = 900
      failure_reset_time_seconds       = 43200
    }

    headers {
      x_frame_options                    = "SAMEORIGIN"
      content_security_policy            = "frame-src 'self'; frame-ancestors 'self'; object-src 'none';"
      x_content_type_options             = "nosniff"
      x_xss_protection                   = "1; mode=block"
      strict_transport_security          = "max-age=31536000; includeSubDomains"
      referrer_policy                    = "no-referrer"
    }
  }

  internationalization {
    supported_locales = ["en"]
    default_locale    = "en"
  }
}


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  2. REALM ROLES                                                          ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

resource "keycloak_role" "realm_roles" {
  for_each = var.realm_roles

  realm_id    = keycloak_realm.app.id
  name        = each.key
  description = each.value
}

# Composite: admin inherits manager, manager inherits user
resource "keycloak_role" "admin_composite" {
  realm_id    = keycloak_realm.app.id
  name        = "admin"
  description = var.realm_roles["admin"]

  composite_roles = [
    keycloak_role.realm_roles["manager"].id,
    keycloak_role.realm_roles["user"].id,
    keycloak_role.realm_roles["viewer"].id,
  ]

  depends_on = [keycloak_role.realm_roles]

  lifecycle {
    # Managed separately from the flat role map
    ignore_changes = []
  }
}

resource "keycloak_role" "manager_composite" {
  realm_id    = keycloak_realm.app.id
  name        = "manager"
  description = var.realm_roles["manager"]

  composite_roles = [
    keycloak_role.realm_roles["user"].id,
    keycloak_role.realm_roles["viewer"].id,
  ]

  depends_on = [keycloak_role.realm_roles]
}

# Default roles assigned to every new user
resource "keycloak_default_roles" "default" {
  realm_id  = keycloak_realm.app.id
  default_roles = [
    "offline_access",
    "uma_authorization",
  ]
}


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  3. CLIENT SCOPES (shared protocol mappers)                              ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

# ── Roles scope: include realm roles in the JWT ──────────────────────────
resource "keycloak_openid_client_scope" "roles_scope" {
  realm_id               = keycloak_realm.app.id
  name                   = "app-roles"
  description            = "Maps realm roles into the JWT access token"
  include_in_token_scope = true
}

resource "keycloak_openid_user_realm_role_protocol_mapper" "realm_roles_mapper" {
  realm_id        = keycloak_realm.app.id
  client_scope_id = keycloak_openid_client_scope.roles_scope.id
  name            = "realm-roles"

  claim_name          = "roles"
  claim_value_type    = "String"
  multivalued         = true
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

# ── User attributes scope ────────────────────────────────────────────────
resource "keycloak_openid_client_scope" "user_attributes_scope" {
  realm_id               = keycloak_realm.app.id
  name                   = "user-attributes"
  description            = "Adds user profile attributes to the JWT"
  include_in_token_scope = true
}

resource "keycloak_openid_user_attribute_protocol_mapper" "email_verified" {
  realm_id        = keycloak_realm.app.id
  client_scope_id = keycloak_openid_client_scope.user_attributes_scope.id
  name            = "email-verified"

  user_attribute       = "emailVerified"
  claim_name           = "email_verified"
  claim_value_type     = "boolean"
  add_to_id_token      = true
  add_to_access_token  = false
  add_to_userinfo      = true
}


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  4. BACKEND CLIENT — Spring Boot (confidential, bearer-only)             ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

resource "keycloak_openid_client" "backend" {
  realm_id  = keycloak_realm.app.id
  client_id = var.backend_client_id
  name      = "Spring Boot API"
  enabled   = true

  access_type                  = "CONFIDENTIAL"
  standard_flow_enabled        = false
  implicit_flow_enabled        = false
  direct_access_grants_enabled = false
  service_accounts_enabled     = true

  root_url  = var.backend_root_url
  base_url  = var.backend_root_url
  admin_url = var.backend_root_url

  valid_redirect_uris = ["${var.backend_root_url}/*"]
  web_origins         = ["+"] # Allow CORS from all valid redirect URIs
}

# Attach custom scopes to the backend client
resource "keycloak_openid_client_default_scopes" "backend_scopes" {
  realm_id  = keycloak_realm.app.id
  client_id = keycloak_openid_client.backend.id

  default_scopes = [
    "openid",
    "profile",
    "email",
    keycloak_openid_client_scope.roles_scope.name,
    keycloak_openid_client_scope.user_attributes_scope.name,
  ]
}

# ── Backend-specific audience mapper ─────────────────────────────────────
resource "keycloak_openid_audience_protocol_mapper" "backend_audience" {
  realm_id  = keycloak_realm.app.id
  client_id = keycloak_openid_client.backend.id
  name      = "backend-audience"

  included_client_audience = var.backend_client_id
  add_to_id_token          = false
  add_to_access_token      = true
}


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  5. FRONTEND CLIENT — SPA (public, Authorization Code + PKCE)            ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

resource "keycloak_openid_client" "frontend" {
  realm_id  = keycloak_realm.app.id
  client_id = var.frontend_client_id
  name      = "Frontend Application"
  enabled   = true

  access_type                  = "PUBLIC"
  standard_flow_enabled        = true
  implicit_flow_enabled        = false # PKCE replaces implicit
  direct_access_grants_enabled = false

  root_url = var.frontend_root_url
  base_url = var.frontend_root_url

  valid_redirect_uris = var.frontend_valid_redirects
  valid_post_logout_redirect_uris = ["${var.frontend_root_url}/*"]
  web_origins         = var.frontend_web_origins

  # ── PKCE enforcement ─────────────────────────────────────────────────
  pkce_code_challenge_method = "S256"
}

# Attach custom scopes to the frontend client
resource "keycloak_openid_client_default_scopes" "frontend_scopes" {
  realm_id  = keycloak_realm.app.id
  client_id = keycloak_openid_client.frontend.id

  default_scopes = [
    "openid",
    "profile",
    "email",
    keycloak_openid_client_scope.roles_scope.name,
    keycloak_openid_client_scope.user_attributes_scope.name,
  ]
}

# ── Audience mapper so the FE token includes the backend audience ────────
resource "keycloak_openid_audience_protocol_mapper" "frontend_audience" {
  realm_id  = keycloak_realm.app.id
  client_id = keycloak_openid_client.frontend.id
  name      = "backend-audience"

  included_client_audience = var.backend_client_id
  add_to_id_token          = false
  add_to_access_token      = true
}


# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║  6. DEMO USERS                                                           ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

resource "keycloak_user" "demo_users" {
  for_each = var.create_demo_users ? {
    for u in var.demo_users : u.username => u
  } : {}

  realm_id = keycloak_realm.app.id
  username = each.value.username
  enabled  = true

  email      = each.value.email
  first_name = each.value.first_name
  last_name  = each.value.last_name

  initial_password {
    value     = each.value.password
    temporary = false
  }
}

# Assign roles to each demo user
resource "keycloak_user_roles" "demo_user_roles" {
  for_each = var.create_demo_users ? {
    for u in var.demo_users : u.username => u
  } : {}

  realm_id = keycloak_realm.app.id
  user_id  = keycloak_user.demo_users[each.key].id

  role_ids = [
    for role_name in each.value.roles :
    keycloak_role.realm_roles[role_name].id
  ]

  depends_on = [keycloak_role.realm_roles]
}
