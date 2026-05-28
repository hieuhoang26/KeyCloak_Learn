locals {
  # Merge all role resources into one map for lookup in user_roles
  all_roles = merge(
    { for k, v in keycloak_role.realm_roles : k => v.id },
    {
      "manager" = keycloak_role.manager_composite.id
      "admin"   = keycloak_role.admin_composite.id
    }
  )
}

resource "keycloak_user" "demo_users" {
  for_each = var.create_demo_users ? {
    for u in var.demo_users : u.username => u
  } : {}

  realm_id   = keycloak_realm.demo-realm.id
  username   = each.value.username
  enabled    = true

  email      = each.value.email
  first_name = each.value.first_name
  last_name  = each.value.last_name
  initial_password {
    value     = each.value.password
    temporary = false
  }
}

resource "keycloak_user_roles" "demo_user_roles" {
  for_each = var.create_demo_users ? {
    for u in var.demo_users : u.username => u
  } : {}

  realm_id = keycloak_realm.demo-realm.id
  user_id  = keycloak_user.demo_users[each.key].id

  role_ids = [
    for role_name in each.value.roles :
    local.all_roles[role_name]
  ]

  depends_on = [keycloak_role.realm_roles, keycloak_role.manager_composite, keycloak_role.admin_composite]
}
