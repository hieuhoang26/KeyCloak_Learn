keycloak_url      = "http://localhost:8080"
keycloak_username = "admin"
keycloak_password = "admin"
realm_roles = {
  "admin"   = "Full administrative access"
  "user"    = "Standard user access"
  "manager" = "Can manage resources and users"
  "viewer"  = "Read-only access"
}

# be
be_client_id  = "spring-boot-api"
be_root_url = "http://localhost:8081"


# ---
# manager
# ├── user   (inherited: false) ← directly assigned to manager
# └── viewer (inherited: false) ← directly assigned to manager
#
# admin
# └── manager (inherited: false) ← directly assigned to admin
# ├── user   (inherited: true) ← comes via manager
# └── viewer (inherited: true) ← comes via manager
