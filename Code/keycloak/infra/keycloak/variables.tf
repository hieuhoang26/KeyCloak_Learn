variable "keycloak_url" {
  type = string
  description = "Base URL of the Keycloak server"
  default = "http://localhost:8080"
}

variable "keycloak_username" {
  type = string
  description = "Username"
}

variable "keycloak_password" {
  type = string
  description = "Password"
}

variable "realm_roles" {
  type = map(string)
  description = "realm roles"
  default = {
    "admin"     = "Full administrative access"
    "user"      = "Standard user access"
  }
}

# BE client
variable "be_client_id" {
  type = string
  description = "BE Client Id"
  default = "spring-boot"
}

variable "be_client_secret" {
  type = string
  description = "BE Client Secret"
  default = "secret"
}

variable "be_root_url" {
  type = string
  description = "BE Client Url"
  default = "http://localhost:8000"
}

# demo user
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
  default = [
    {
      username   = "admin-user"
      email      = "admin@example.com"
      first_name = "Admin"
      last_name  = "User"
      password   = "Admin123!"
      roles      = ["admin"]
    },
    {
      username   = "manager-user"
      email      = "manager@example.com"
      first_name = "Manager"
      last_name  = "User"
      password   = "Manager123!"
      roles      = ["manager"]
    },
    {
      username   = "regular-user"
      email      = "user@example.com"
      first_name = "Regular"
      last_name  = "User"
      password   = "User1234!"
      roles      = ["user"]
    },
    {
      username   = "viewer-user"
      email      = "viewer@example.com"
      first_name = "Viewer"
      last_name  = "User"
      password   = "Viewer123!"
      roles      = ["viewer"]
    }
  ]
}