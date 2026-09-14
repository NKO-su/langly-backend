# Authentication

## 1. Local Registration

### Flow

Client
↓
AuthController
↓
RegisterRequest
↓
AuthService.register()
↓
Repositories / PasswordEncoder / EmailService

---

### Controller Responsibility

`AuthController` chịu trách nhiệm:

- Nhận HTTP request
- Nhận `RegisterRequest`
- Gọi `AuthService`
- Trả HTTP response

Controller không xử lý trực tiếp:

- Database
- Password hashing
- Token generation
- Email sending

---

### Service Responsibility

`AuthService` chứa business logic của authentication.

Khi gọi:

```java
authService.register(email, password);