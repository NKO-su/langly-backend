# Langly Backend — Authentication Module (Tuần 1)

Module xử lý đăng ký (local, kèm xác nhận email thật) và đăng nhập
(JWT access token + refresh token) cho ứng dụng Langly.

## Tech stack liên quan

- Spring Boot, Spring Security, Spring Data JPA
- PostgreSQL
- BCrypt (`PasswordEncoder`) — hash password
- JWT (`io.jsonwebtoken`) — access token + refresh token
- Gmail SMTP (`spring-boot-starter-mail`) — gửi email xác nhận thật

## Tính năng

### Đăng ký (Local Register + Email Verification)

Không tạo tài khoản ngay khi submit form — chỉ tạo sau khi user xác
nhận email qua link được gửi tới hộp thư thật.

1. `POST /api/auth/register` — nhận `email` + `password`
2. Kiểm tra email chưa tồn tại trong `users`, chưa có yêu cầu đăng ký
   nào đang chờ xác nhận
3. Hash password, sinh token ngẫu nhiên (`SecureRandom`, 256-bit),
   lưu tạm vào `pending_registrations` (hết hạn sau 30 phút)
4. Gửi email chứa link xác nhận qua Gmail SMTP
5. `GET /api/auth/verify?token=...` — xác nhận đúng token, tạo
   `User` thật (`provider = LOCAL`), xóa record tạm
6. `POST /api/auth/resend-verification?email=...` — gửi lại email nếu
   chưa nhận được, giới hạn: cooldown 60 giây, tối đa 5 lần / 30 phút

### Đăng nhập (JWT + Refresh Token)

1. `POST /api/auth/login` — nhận `email` + `password`
2. Xác thực bằng `PasswordEncoder.matches()`; sai email hoặc sai
   password đều trả về cùng một thông báo lỗi (chống dò email)
3. Trả về **access token** (JWT, hết hạn sau 1 giờ, không lưu DB) và
   **refresh token** (hết hạn sau 6 tháng, lưu trong bảng
   `refresh_tokens` để server có thể chủ động thu hồi)
4. `POST /api/auth/refresh` — dùng refresh token còn hợp lệ để xin
   cấp access token mới, không cần đăng nhập lại
5. Mọi API khác (ngoài `/api/auth/**`) bắt buộc phải có access token
   hợp lệ trong header `Authorization: Bearer <token>`, được kiểm tra
   bởi `JwtAuthenticationFilter`

## Cấu trúc thư mục liên quan

```
com.langly.langly_backend
├── model/
│   ├── User.java
│   ├── AuthProvider.java              (LOCAL / GOOGLE / LINKED)
│   ├── PendingRegistration.java
│   └── RefreshToken.java
├── repository/
│   ├── UserRepository.java
│   ├── PendingRegistrationRepository.java
│   └── RefreshTokenRepository.java
├── util/
│   ├── TokenGenerator.java            (static, SecureRandom)
│   └── JwtUtil.java                   (@Component, sinh/verify JWT)
├── service/
│   ├── EmailService.java
│   └── AuthService.java
├── controller/
│   └── AuthController.java
├── config/
│   ├── SecurityConfig.java
│   └── JwtAuthenticationFilter.java
├── dto/
│   ├── RegisterRequest.java / LoginRequest.java / RefreshTokenRequest.java
│   └── AuthResponse.java
└── exception/
    └── EmailAlreadyExistsException.java
```

## Cấu hình môi trường cần thiết

Đặt các biến sau qua environment variables (không hard-code vào
`application.properties`):

| Biến | Mô tả |
|---|---|
| `MAIL_USERNAME` | Địa chỉ Gmail dùng để gửi mail xác nhận |
| `MAIL_PASSWORD` | Gmail App Password (không phải mật khẩu Gmail thật) |
| `JWT_SECRET` | Secret key ký JWT (chuỗi ngẫu nhiên ≥ 256-bit, Base64) |

## Ghi chú thiết kế đáng nhớ

- Access token không lưu DB (stateless) — verify chỉ dựa vào chữ ký +
  hạn dùng ghi sẵn trong token.
- Refresh token bắt buộc lưu DB — đây là cơ chế duy nhất cho phép
  server thu hồi phiên đăng nhập trước khi hết hạn tự nhiên.
- `AuthProvider` đã có sẵn `LINKED` để chuẩn bị cho Google OAuth2
  (Tuần 2): nếu Google trả về email trùng với tài khoản LOCAL đã có,
  hệ thống tự động hợp nhất 2 tài khoản.

