## 1.Integrate a Profile Service with KeyCloak

```bash
docker pull quay.io/keycloak/keycloak:26.4.2

docker run -d --name keycloak -p 127.0.0.1:8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.4.2 start-dev
```

> http://localhost:8080/admin

- Create admin acc `admin_root`
- Create realm
- Create client
- Create users

```bash
# exchange token
curl -X POST "http://localhost:8080/realms/<realm-name>/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=<client-id>" \
  -d "client_secret=<client-secret>" \
  -d "username=<username>" \
  -d "password=<password>" \
  -d "scope=openid"


# get user info
curl -X GET "http://localhost:8080/realms/<realm-name>/protocol/openid-connect/userinfo" \
  -H "Authorization: Bearer <access-token>"


# dicovery endpoint
curl -X GET "http://localhost:8180/realms/devteria/.well-known/openid-configuration"

```

## 2. Build Identity and Profile management capabilities

Docs - Administration REST API - OpenAPI definition in YAML format

3. Config Spring Security with KeyCloak
4. Config KeyCloak login with ReactJs Web-app
5. Configure SSO
6. Social Login with Google, Facebook ...
7. Configure KeyCloak ready for Production
   - Security
   - Database
   - Monitoring

| DB RBAC          | Keycloak                                              |
| ---------------- | ----------------------------------------------------- |
| users            | Users                                                 |
| roles            | Realm Roles (global) / Client Role (permission-level) |
| permissions      | Client Roles (scope-level)                            |
| user_roles       | User ↔ Role mapping                                   |
| role_permissions | Role composite                                        |
