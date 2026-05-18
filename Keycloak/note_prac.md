# ✅ **1. Không nên: Lấy admin token bằng username/password mỗi lần**

Cách này **không an toàn**, **không hiệu quả**, và **tốn chi phí network**:

- Mỗi request lại gọi `/token` → tốn thời gian.
- Lộ `admin-username` + `admin-password` → cực kỳ nguy hiểm.
- Khó scaling khi deploy nhiều instance.

---

# ✅ **2. Best Practice: Dùng Client Credentials (Service Account)**

Cách chuẩn nhất là tạo một **Confidential Client** trong Keycloak và bật **Service Account**.

Client này sẽ đại diện cho backend và có quyền quản trị kiểu:

- manage-users
- view-users
- manage-realm
- manage-clients
- v.v.

### 🔧 **Cách thực hiện**

## **Step 1: Tạo client**

`Clients → Create`

- Client ID: `admin-service`
- Client Type: Confidential
- Access Type: Confidential
- Standard Flow: OFF
- Direct Access Grants: OFF
- Service Accounts Enabled: ON

## **Step 2: Cấp quyền cho service account**

`Clients → admin-service → Service Account Roles`

Add role:

- realm-management → **manage-users**
- realm-management → **view-users**

## **Step 3: Lấy token bằng client_credentials**

Backend chỉ cần lưu:

- client_id
- client_secret

API token:

```
POST /realms/{realm}/protocol/openid-connect/token
grant_type=client_credentials
client_id=admin-service
client_secret=XXXXXX
```

→ Trả về access_token (admin token).

### Điều quan trọng:

**Token này có TTL (mặc định 1 giờ).**
Bạn có thể cache hoặc set refresh logic.

---

# ✅ **3. Best Practice trong backend**

## **Cache token (per instance)**

Ví dụ trong Spring WebFlux:

```java
private Mono<String> adminToken = Mono.defer(this::fetchAdminToken)
        .cache(Duration.ofMinutes(50)); // TTL < token TTL
```

`fetchAdminToken()` gọi token API 1 lần, dùng lại cả giờ.

Nếu lỗi "token expired" → refresh.

---

# ✅ **4. Tuyệt đối không dùng admin user để gọi API**

Không dùng:

✔ `admin`
✔ `admin@master`
✔ tài khoản người thật

→ luôn dùng **Service Account**.

---

# ✅ **5. Không gọi trực tiếp API?**

Nếu dùng Spring Boot thì best practice hơn nữa:

## **Dùng Keycloak Admin Client SDK chính thức**

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>x.x.x</version>
</dependency>
```

Sử dụng:

```java
Keycloak keycloak = KeycloakBuilder.builder()
        .serverUrl(url)
        .realm("master")
        .clientId("admin-service")
        .clientSecret(secret)
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .build();
```

→ Tự lo caching token + refresh
→ Code gọn hơn nhiều
→ Không cần tự call REST

---

# 🚀 **Tóm tắt best practice**

| Mức độ       | Cách làm                                                       | Ghi chú                      |
| ------------ | -------------------------------------------------------------- | ---------------------------- |
| ❌ Tệ        | Gọi token bằng (admin username/password) mỗi request           | Không nên                    |
| ⚠ Tạm được   | Gọi token admin và tự cache                                    | Ok nhưng vẫn dùng admin user |
| ✔ Tốt nhất   | **Confidential Client → Service Account → Client Credentials** | Chuẩn Keycloak               |
| ⭐ Tuyệt vời | **Dùng Keycloak Admin SDK**                                    | Auto refresh token           |

---

# Nếu muốn mình cho luôn mẫu code WebFlux (Mono) chuẩn nhất với caching token + retry khi token hết hạn, chỉ cần nói “code mẫu WebFlux”.
