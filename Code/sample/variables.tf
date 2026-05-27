# =============================================================================
# Variables — Keycloak Terraform Configuration
# Spring Boot + JWT + Role-Based Auth + Client Frontend
# =============================================================================

# ── Keycloak Provider ────────────────────────────────────────────────────────

variable "keycloak_url" {
  description = "Base URL of the Keycloak server (e.g. http://localhost:8080)"
  type        = string
  default     = "http://localhost:8080"
}

variable "keycloak_admin_user" {
  description = "Keycloak admin username"
  type        = string
  default     = "admin"
}

variable "keycloak_admin_password" {
  description = "Keycloak admin password"
  type        = string
  sensitive   = true
}

# ── Realm ────────────────────────────────────────────────────────────────────

variable "realm_name" {
  description = "Name of the Keycloak realm"
  type        = string
  default     = "app-realm"
}

variable "realm_display_name" {
  description = "Display name shown on the login page"
  type        = string
  default     = "My Application"
}

# ── Backend (Spring Boot) Client ─────────────────────────────────────────────

variable "backend_client_id" {
  description = "Client ID for the Spring Boot backend (confidential)"
  type        = string
  default     = "spring-boot-api"
}

variable "backend_client_secret" {
  description = "Client secret for the Spring Boot backend"
  type        = string
  sensitive   = true
  default     = null # auto-generated if null
}

variable "backend_root_url" {
  description = "Root URL of the Spring Boot backend"
  type        = string
  default     = "http://localhost:8081"
}

# ── Frontend Client ──────────────────────────────────────────────────────────

variable "frontend_client_id" {
  description = "Client ID for the frontend SPA (public)"
  type        = string
  default     = "frontend-app"
}

variable "frontend_root_url" {
  description = "Root URL of the frontend application"
  type        = string
  default     = "http://localhost:3000"
}

variable "frontend_valid_redirects" {
  description = "Valid redirect URIs for the frontend client"
  type        = list(string)
  default     = ["http://localhost:3000/*"]
}

variable "frontend_web_origins" {
  description = "Allowed CORS origins for the frontend client"
  type        = list(string)
  default     = ["http://localhost:3000"]
}

# ── Roles ────────────────────────────────────────────────────────────────────

variable "realm_roles" {
  description = "Map of realm role names to their descriptions"
  type        = map(string)
  default = {
    "admin"     = "Full administrative access"
    "manager"   = "Can manage resources and users"
    "user"      = "Standard user access"
    "viewer"    = "Read-only access"
  }
}

variable "default_roles" {
  description = "Roles automatically assigned to new users"
  type        = list(string)
  default     = ["user"]
}

# ── Token Lifespans ──────────────────────────────────────────────────────────

variable "access_token_lifespan" {
  description = "Access token lifespan in seconds (default 5 min)"
  type        = number
  default     = 300
}

variable "refresh_token_lifespan" {
  description = "Refresh token (SSO session idle) lifespan in seconds (default 30 min)"
  type        = number
  default     = 1800
}

variable "sso_session_max_lifespan" {
  description = "Max SSO session lifespan in seconds (default 10 hours)"
  type        = number
  default     = 36000
}

# ── Demo Users ───────────────────────────────────────────────────────────────

variable "create_demo_users" {
  description = "Whether to create demo users for testing"
  type        = bool
  default     = true
}

variable "demo_users" {
  description = "Demo users to create (only if create_demo_users = true)"
  type = list(object({
    username   = string
    email      = string
    first_name = string
    last_name  = string
    password   = string
    roles      = list(string)
  }))
  sensitive = true
  default = [
    {
      username   = "admin-user"
      email      = "admin@example.com"
      first_name = "Admin"
      last_name  = "User"
      password   = "admin123"
      roles      = ["admin", "manager", "user"]
    },
    {
      username   = "manager-user"
      email      = "manager@example.com"
      first_name = "Manager"
      last_name  = "User"
      password   = "manager123"
      roles      = ["manager", "user"]
    },
    {
      username   = "regular-user"
      email      = "user@example.com"
      first_name = "Regular"
      last_name  = "User"
      password   = "user123"
      roles      = ["user"]
    },
    {
      username   = "viewer-user"
      email      = "viewer@example.com"
      first_name = "Viewer"
      last_name  = "User"
      password   = "viewer123"
      roles      = ["viewer"]
    }
  ]
}
