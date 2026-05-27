# =============================================================================
# Outputs
# =============================================================================

# ── Realm ────────────────────────────────────────────────────────────────────

output "realm_id" {
  description = "Keycloak realm ID"
  value       = keycloak_realm.app.id
}

output "realm_name" {
  description = "Keycloak realm name"
  value       = keycloak_realm.app.realm
}

# ── OIDC Discovery ──────────────────────────────────────────────────────────

output "issuer_url" {
  description = "OIDC issuer URL (for Spring Boot spring.security.oauth2.resourceserver.jwt.issuer-uri)"
  value       = "${var.keycloak_url}/realms/${var.realm_name}"
}

output "jwks_uri" {
  description = "JWKS URI for JWT verification"
  value       = "${var.keycloak_url}/realms/${var.realm_name}/protocol/openid-connect/certs"
}

output "token_endpoint" {
  description = "Token endpoint"
  value       = "${var.keycloak_url}/realms/${var.realm_name}/protocol/openid-connect/token"
}

output "authorization_endpoint" {
  description = "Authorization endpoint (for frontend PKCE flow)"
  value       = "${var.keycloak_url}/realms/${var.realm_name}/protocol/openid-connect/auth"
}

output "logout_endpoint" {
  description = "Logout endpoint"
  value       = "${var.keycloak_url}/realms/${var.realm_name}/protocol/openid-connect/logout"
}

# ── Clients ──────────────────────────────────────────────────────────────────

output "backend_client_id" {
  description = "Backend client ID"
  value       = keycloak_openid_client.backend.client_id
}

output "backend_client_secret" {
  description = "Backend client secret (use in Spring Boot application.yml)"
  value       = keycloak_openid_client.backend.client_secret
  sensitive   = true
}

output "frontend_client_id" {
  description = "Frontend client ID (use in your JS/React OIDC config)"
  value       = keycloak_openid_client.frontend.client_id
}

# ── Spring Boot Config Snippet ───────────────────────────────────────────────

output "spring_boot_config" {
  description = "application.yml snippet for Spring Boot"
  value       = <<-YAML
    # ── Spring Boot application.yml ──────────────────────
    spring:
      security:
        oauth2:
          resourceserver:
            jwt:
              issuer-uri: ${var.keycloak_url}/realms/${var.realm_name}
              jwk-set-uri: ${var.keycloak_url}/realms/${var.realm_name}/protocol/openid-connect/certs
  YAML
}

# ── Frontend Config Snippet ──────────────────────────────────────────────────

output "frontend_oidc_config" {
  description = "OIDC config for frontend (e.g. oidc-client-ts / keycloak-js)"
  value = {
    authority   = "${var.keycloak_url}/realms/${var.realm_name}"
    client_id   = var.frontend_client_id
    redirect_uri = "${var.frontend_root_url}/callback"
    post_logout_redirect_uri = var.frontend_root_url
    scope        = "openid profile email app-roles"
    response_type = "code"
    pkce_method   = "S256"
  }
}
