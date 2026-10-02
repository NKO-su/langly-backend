# Authentication Review — Langly Backend

## 1. Authentication Architecture

Authentication-related request flow:

```text
Mobile App
   ↓
Controller
   ↓
DTO validation
   ↓
AuthService
   ↓
Repository / PasswordEncoder / JwtUtil / EmailService
   ↓
Database / Email Provider
   ↓
Response
```

Controller is responsible for HTTP input/output. Business logic belongs to `AuthService`. Repository handles persistence. DTO represents API request/response data.

---

## 2. Registration Flow

### Endpoint

```http
POST /api/auth/register
```

The client sends registration data as JSON.

```java
@PostMapping("/register")
public ResponseEntity<String> register(
        @Valid @RequestBody RegisterRequest request)
```

### Request flow

```text
JSON request
    ↓
@RequestBody
    ↓
RegisterRequest
    ↓
@Valid
    ↓
AuthService.register(email, password)
```

`@Valid` validates the DTO before the service method is executed. Invalid input is rejected early.

### `AuthService.register()`

The registration process:

1. Check whether the email already exists in `User`.
2. Search for an existing `PendingRegistration`.
3. If a pending registration exists:
   - valid pending registration → reject a duplicate registration attempt;
   - expired pending registration → delete the old record.
4. Hash the raw password with `PasswordEncoder`.
5. Generate an email-verification token.
6. Create a `PendingRegistration`.
7. Save it to the database.
8. Send the verification email.

```text
UserRepository.existsByEmail()
        ↓
PendingRegistrationRepository.findByEmail()
        ↓
PasswordEncoder.encode()
        ↓
TokenGenerator.generateToken()
        ↓
new PendingRegistration(...)
        ↓
pendingRegistrationRepository.save()
        ↓
EmailService.sendVerificationEmail()
```

The password stored in `PendingRegistration` is already hashed. The raw password is not stored.

---

## 3. `PendingRegistration`

`PendingRegistration` represents a registration that has not yet become a real `User`.

It is stored in the `pending_registrations` table.

Important fields:

| Field | Purpose |
|---|---|
| `id` | Record identifier |
| `email` | Email being registered |
| `password` | Hashed password |
| `token` | Email verification token |
| `expiresAt` | Expiration time |
| `resendCount` | Number of resend operations |
| `lastSentAt` | Time of the latest email |

A pending registration is effectively in the "waiting for confirmation" state because the record exists in `pending_registrations`. A separate `status` field is not used.

### Expiration

```java
public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
}
```

```java
!pending.isExpired()
```

means that the pending registration has not expired.

### Resend cooldown

```java
public boolean isInCooldown() {
    return LocalDateTime.now()
            .isBefore(lastSentAt.plusSeconds(60));
}
```

A resend is blocked during the 60-second cooldown after the previous email.

### Resend limit

```java
public boolean hasReachedResendLimit() {
    return resendCount >= 5;
}
```

The record allows a maximum of five resend operations according to the current implementation.

### Token regeneration

```java
public void regenerateToken(String newToken) {
    this.token = newToken;
    this.expiresAt = LocalDateTime.now().plusMinutes(30);
    this.resendCount++;
    this.lastSentAt = LocalDateTime.now();
}
```

A resend changes four related pieces of state together:

- token
- expiration time
- resend count
- last sent time

Keeping this operation inside `PendingRegistration` prevents the service from having to update these fields independently and reduces the possibility of forgetting one of the required state changes.

---

## 4. Email Verification

### Endpoint

```http
GET /api/auth/verify?token=...
```

The token is received with `@RequestParam` because it is part of the URL query string.

Flow:

```text
Email verification link
        ↓
AuthController.verify(token)
        ↓
AuthService.verifyEmail(token)
        ↓
PendingRegistrationRepository.findByToken()
        ↓
Check expiration
        ↓
Create User
        ↓
Save User
        ↓
Delete PendingRegistration
```

If the token does not exist, verification fails.

If the token is expired, the pending record is deleted and verification fails.

If verification succeeds:

```java
User newUser = new User(
    pending.getEmail(),
    pending.getPassword(),
    AuthProvider.LOCAL
);
```

The real `User` is created using the already-hashed password.

The pending registration is then deleted because its temporary purpose is complete.

---

## 5. EmailService

`EmailService` isolates email-sending infrastructure from authentication business logic.

```text
AuthService
    ↓
EmailService
    ↓
JavaMailSender
    ↓
Email
```

`AuthService` decides when an email must be sent. `EmailService` handles the mechanics of constructing and sending the email.

The verification URL currently uses:

```text
http://localhost:8080/api/auth/verify?token=...
```

This is suitable for local development. A deployed environment requires an environment-appropriate URL.

---

## 6. Login Flow

### Endpoint

```http
POST /api/auth/login
```

The client sends email and password as JSON.

```text
JSON
  ↓
LoginRequest
  ↓
@Valid
  ↓
AuthController
  ↓
AuthService.login()
```

### `AuthService.login()`

1. Find the user by email.
2. If the user does not exist, reject authentication.
3. Compare the raw password with the stored password hash.
4. Generate an Access Token.
5. Generate a Refresh Token.
6. Calculate the refresh-token expiration time.
7. Create a `RefreshToken` entity associated with the `User`.
8. Save the refresh token to the database.
9. Return `AuthResponse`.

### Password verification

```java
passwordEncoder.matches(rawPassword, user.getPassword())
```

This does not decrypt the stored password.

The operation verifies whether the supplied raw password matches the stored hash.

### Access Token

The Access Token is used by the application when calling protected APIs.

### Refresh Token

The Refresh Token is used to obtain a new Access Token after the Access Token expires.

The backend stores the Refresh Token in the database so that the token can be associated with a user and managed/revoked as a server-side session credential.

---

## 7. Refresh Token Flow

### Endpoint

```http
POST /api/auth/refresh
```

Flow:

```text
Refresh Token
    ↓
RefreshTokenRepository.findByToken()
    ↓
Check expiration
    ↓
Get associated User
    ↓
Generate new Access Token
    ↓
Return AuthResponse
```

The `RefreshToken` entity contains a reference to the owning `User`.

Therefore:

```java
User user = refreshToken.getUser();
```

identifies which user should receive the new Access Token.

An expired refresh token is deleted and rejected.

---

## 8. Logout Flow

### Endpoint

```http
POST /api/auth/logout
```

The logout operation searches for the supplied Refresh Token and deletes it if present.

Deleting the stored Refresh Token means that the same token can no longer be used through the database-backed refresh mechanism.

The current implementation therefore treats the Refresh Token as the server-revocable authentication session credential.

---

## 9. Google OAuth2 Login

The OAuth2 flow is different from local password authentication because Google performs the identity authentication.

```text
Client
  ↓
Google OAuth2
  ↓
Spring Security
  ↓
OAuth2SuccessHandler
  ↓
AuthService.oauth2Login(email)
  ↓
User lookup / creation / linking
  ↓
Access + Refresh Token
  ↓
HTTP JSON response
  ↓
Client
```

### `OAuth2SuccessHandler`

After successful OAuth2 authentication:

```java
OAuth2User oAuth2User =
    (OAuth2User) authentication.getPrincipal();

String email =
    oAuth2User.getAttribute("email");
```

The email is used as the identifier for the application user.

The handler then delegates business logic:

```java
AuthResponse authResponse =
    authService.oauth2Login(email);
```

The handler does not create users or JWTs itself.

### `AuthService.oauth2Login()`

If no user exists for the email:

```text
create User(email, null, GOOGLE)
```

If a LOCAL user already exists with the same email:

```text
LOCAL → LINKED
```

This allows the existing application account to be associated with Google authentication instead of creating a duplicate user.

The method then creates an Access Token and Refresh Token, stores the Refresh Token, and returns `AuthResponse`.

---

## 10. SecurityConfig

`SecurityConfig` defines the Spring Security request-protection rules.

Important rules:

```java
.requestMatchers("/api/auth/**").permitAll()
.anyRequest().authenticated()
```

Authentication endpoints are public. Other endpoints require authentication.

JWT authentication is configured with:

```java
.addFilterBefore(
    jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class
)
```

The application uses:

```java
SessionCreationPolicy.STATELESS
```

This means authentication state is not maintained using a traditional HTTP session. Each protected request is authenticated using its credentials, in this project primarily the JWT.

OAuth2 success handling is connected with:

```java
.oauth2Login(oauth2 ->
    oauth2.successHandler(oAuth2SuccessHandler)
)
```

---

## 11. JwtAuthenticationFilter

The filter processes each request before protected controller logic.

Flow:

```text
HTTP Request
    ↓
Authorization header
    ↓
Check "Bearer "
    ↓
Extract JWT
    ↓
JwtUtil.isTokenValid()
    ↓
JwtUtil.extractEmail()
    ↓
Create Authentication
    ↓
SecurityContextHolder.setAuthentication()
    ↓
Continue filter chain
```

The important line is:

```java
SecurityContextHolder
    .getContext()
    .setAuthentication(authentication);
```

This tells Spring Security that the current request has been authenticated as the extracted user identity.

The filter does not decide which endpoints are public. `SecurityConfig` defines that policy.

---

## 12. JwtUtil

`JwtUtil` centralizes JWT generation and validation.

It stores:

- signing `SecretKey`
- Access Token expiration
- Refresh Token expiration

Configuration values are injected using `@Value`.

### Token generation

Both token types use the same generation method:

```java
generateToken(email, expirationMs)
```

The difference is their expiration duration.

A generated JWT contains:

- subject: email
- issued-at time
- expiration time
- signature created with the secret key

### Token validation

```java
public boolean isTokenValid(String token)
```

The token is parsed and its signature is verified using the same secret key.

The expiration is then checked.

Invalid parsing, invalid signature, or other parsing/validation exceptions cause the method to return `false`.

### JWT request flow

```text
Client
  ↓
Authorization: Bearer <JWT>
  ↓
JwtAuthenticationFilter
  ↓
JwtUtil.isTokenValid()
  ↓
JwtUtil.extractEmail()
  ↓
Authentication
  ↓
SecurityContext
  ↓
Protected Controller
```

---

## 13. Repository Role

Repositories form the persistence layer between services and the database.

Example:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

Important Spring Data patterns:

```text
findByX(...)
existsByX(...)
save(...)
delete(...)
```

`JpaRepository<User, Long>` indicates that the repository manages `User` entities whose identifier type is `Long`.

`Optional` represents the possibility that a matching entity does not exist.

---

## 14. DTO Role

DTOs transfer API data between the client and controller/service boundary.

Example:

```text
Client JSON
    ↓
RegisterRequest
    ↓
@Valid
    ↓
Controller
```

DTOs keep API input/output models separate from database entities.

They also provide a place for request validation.

---

## 15. Architecture Summary

```text
                         ┌───────────────┐
                         │   Mobile App  │
                         └───────┬───────┘
                                 │ HTTP
                                 ↓
                         ┌───────────────┐
                         │  Controller   │
                         └───────┬───────┘
                                 │ DTO
                                 ↓
                         ┌───────────────┐
                         │  AuthService  │
                         └──┬────┬───┬───┘
                            │    │   │
              ┌─────────────┘    │   └──────────────┐
              ↓                  ↓                  ↓
       ┌─────────────┐   ┌──────────────┐   ┌──────────────┐
       │ Repositories│   │ JwtUtil /    │   │ EmailService │
       │             │   │ PasswordEncoder│ │              │
       └──────┬──────┘   └──────────────┘   └──────────────┘
              ↓
          Database
```

The central design principle is separation of responsibilities:

- Controller: HTTP input/output.
- DTO: API data representation and validation.
- Service: authentication business logic.
- Repository: database persistence.
- Entity: domain/database state.
- Security filter: request authentication.
- Security configuration: authentication/authorization rules.
- JWT utility: JWT creation and validation.
- Email service: email delivery.
