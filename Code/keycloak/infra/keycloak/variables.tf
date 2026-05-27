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

