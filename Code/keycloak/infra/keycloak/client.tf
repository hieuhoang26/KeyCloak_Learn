# Client scope

## roles scope (include realm roles in JWT)
resource "keycloak_openid_client_scope" "roles_scope" {
  realm_id               = keycloak_realm.demo-realm.id
  name                   = "app-roles"
  description            = "Maps realm roles into the JWT access token"
  include_in_token_scope = true
}

resource "keycloak_openid_user_realm_role_protocol_mapper" "realm_roles_mapper" {
  realm_id        = keycloak_realm.demo-realm.id
  client_scope_id = keycloak_openid_client_scope.roles_scope.id
  name            = "realm-roles"

  claim_name          = "roles"
  claim_value_type    = "String"
  multivalued         = true
  add_to_id_token     = true
  add_to_access_token = true
  add_to_userinfo     = true
}

## User attributes scope

resource "keycloak_openid_client_scope" "user_attributes_scope" {
  realm_id               = keycloak_realm.demo-realm.id
  name                   = "user-attributes"
  description            = "Adds user profile attributes to the JWT"
  include_in_token_scope = true
}

resource "keycloak_openid_user_attribute_protocol_mapper" "email_verified" {
  realm_id        = keycloak_realm.demo-realm.id
  client_scope_id = keycloak_openid_client_scope.user_attributes_scope.id
  name            = "email-verified"

  user_attribute       = "emailVerified"
  claim_name           = "email_verified"
  claim_value_type     = "boolean"
  add_to_id_token      = true
  add_to_access_token  = false
  add_to_userinfo      = true
}


# Client - BE
resource "keycloak_openid_client" "backend" {
  name = "Spring boot"
  access_type = "CONFIDENTIAL"
  client_id   = var.be_client_id
  realm_id    = keycloak_realm.demo-realm.id

  enabled = true
  standard_flow_enabled = true
  implicit_flow_enabled = true
  direct_access_grants_enabled = true
  service_accounts_enabled = true

  root_url = var.be_root_url
  admin_url = var.be_root_url
  base_url = var.be_root_url

  valid_redirect_uris = ["${var.be_root_url}/*"]
  web_origins         = ["+"] # Allow CORS from all valid redirect URIs

}

# Attach custom scopes to the backend client
resource "keycloak_openid_client_default_scopes" "backend_scopes" {
  realm_id  = keycloak_realm.demo-realm.id
  client_id = keycloak_openid_client.backend.id

  default_scopes = [
    "openid",            # base OIDC, always needed
    "profile",           # name, username, etc.
    "email",             # email address
    keycloak_openid_client_scope.roles_scope.name,            # app-roles
    keycloak_openid_client_scope.user_attributes_scope.name,  # your custom attributes scope
  ]
}

# ── Backend-specific audience mapper ─────────────────────────────────────
resource "keycloak_openid_audience_protocol_mapper" "backend_audience" {
  realm_id  = keycloak_realm.demo-realm.id
  client_id = keycloak_openid_client.backend.id
  name      = "be-aud"

  included_client_audience = var.be_client_id
  add_to_id_token          = false
  add_to_access_token      = true
}

