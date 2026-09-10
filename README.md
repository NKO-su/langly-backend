# Langly Backend — Authentication Module (Tuần 1)

Module xử lý đăng ký (local, kèm xác nhận email thật) và đăng nhập
(JWT access token + refresh token) và đăng nhập bằng Google (OAuth2) cho ứng dụng Langly.

## Tech stack liên quan

- Spring Boot, Spring Security, Spring Data JPA
- PostgreSQL
- BCrypt (`PasswordEncoder`) — hash password
- JWT (`io.jsonwebtoken`) — access token + refresh token
- Gmail SMTP (`spring-boot-starter-mail`) — gửi email xác nhận thật
- **Spring Security OAuth2 Client (`spring-boot-starter-oauth2-client`) — Google Login** *( week 2)*
- **Rome Library (`com.rometools:rome`) — parse RSS feed** *( week 3)*
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

### Đăng nhập bằng Google (OAuth2 Login) — *new, week 2*

Google xác thực danh tính user thay hệ thống; backend chỉ nhận email
đã được xác thực và quyết định gắn vào `User` nào.

1. Client gọi `GET /oauth2/authorization/google` (endpoint do
   `spring-boot-starter-oauth2-client` tự sinh, không tự viết Controller)
2. Redirect sang Google → user đăng nhập & đồng ý quyền (`email`, `profile`)
3. Google redirect về `GET /login/oauth2/code/google` (cũng tự sinh)
4. `OAuth2SuccessHandler.onAuthenticationSuccess()` được Spring gọi lại,
   lấy `email` từ `OAuth2User`, gọi `AuthService.oauth2Login(email)`
5. `AuthService` rẽ 3 nhánh theo `email`:
    - Chưa có `User` → tạo mới, `provider = GOOGLE`
    - Có, đang `LOCAL` → tự động đổi `provider = LINKED` (không cần xác
      nhận password local)
    - Có, đã `GOOGLE`/`LINKED` → dùng luôn, không tạo/sửa gì
6. Sinh access + refresh token (dùng lại nguyên `JwtUtil` của local login)
7. `OAuth2SuccessHandler` tự ghi `AuthResponse` (JSON) trực tiếp vào
   `HttpServletResponse` — vì `onAuthenticationSuccess()` trả `void`,
   không có cơ chế `return` tự serialize như Controller

### Đăng xuất (Logout) 

Vô hiệu hóa refresh token phía server, để nếu token bị lộ thì không
thể dùng để xin access token mới nữa.

1. `POST /api/auth/logout` — nhận `refreshToken` (dùng lại
   `RefreshTokenRequest`, không tạo DTO riêng)
2. Tìm row `RefreshToken` theo giá trị token; nếu có thì xóa, nếu
   không có thì bỏ qua — **idempotent**, gọi bao nhiêu lần cũng trả
   về thành công, không throw lỗi
3. Trả về `204 No Content`
4. Không cần sửa `SecurityConfig` — endpoint rơi vào nhóm
   `/api/auth/**` đã `permitAll()` sẵn, vì bản thân refresh token
   (chuỗi ngẫu nhiên 256-bit) đã là bằng chứng sở hữu đủ mạnh
5. Không làm access-token blacklist — access token ngắn hạn (1h),
   chưa có kịch bản cụ thể cần thu hồi ngay lập tức ở quy mô hiện
   tại (ghi nhận Technical Debt nếu cần sau)

### Bài báo & Lưu bài (Article / SavedArticle) — *(week 3)*

`Article` là dữ liệu **dùng chung** cho mọi user (không gắn `User`
trực tiếp). Việc "lưu" một bài được tách thành entity trung gian
riêng (`SavedArticle`), đại diện cho quan hệ nhiều-nhiều
`User` ↔ `Article`.

- `Article`: `id`, `title`, `description` (TEXT), `link` (unique),
  `pubDate`, `source`, `createdAt` (`@CreationTimestamp`)
- `SavedArticle`: `id`, `user` (`@ManyToOne`), `article`
  (`@ManyToOne`), `savedAt` (`@CreationTimestamp`);
  `@UniqueConstraint({user_id, article_id})` chống lưu trùng
  `POST /api/saved-articles/toggle` — 1 endpoint duy nhất (toggle) cho
  cả lưu và bỏ lưu:
1. Nhận `articleId` (body), xác định `user` qua JWT
   (`Authentication.getName()`, không nhận `userId` từ client)
2. `findByUserAndArticle(user, article)` — có thì xóa (unsave, trả
   `saved: false`), không có thì tạo mới (save, trả `saved: true`)
3. Check tồn tại **trước khi** insert (không insert-rồi-bắt-lỗi)

### Lấy bài từ RSS & lưu tự động (RssFeedService / RssFetchScheduler) — *(week 3)*

- `RssFeedService.fetchAndSave(feedUrl, sourceName)` — đọc **1 feed
  bất kỳ** qua Rome Library (`SyndFeedInput` + `XmlReader`), không
  biết/không quan tâm đang đọc nguồn nào (nhận qua tham số):
   1. Lọc theo `pubDate` — chỉ lấy bài trong vòng **48 giờ gần đây**
      (loại các mục không phải tin thời sự như app-promo, chương
      trình cũ, phát hiện được từ dữ liệu RSS thật của BBC)
   2. `existsByLink()` — chống trùng trước khi insert (kể cả khi 1
      bài xuất hiện 2 lần trong cùng 1 response XML)
   3. Map `SyndEntry` → `Article` (`Date` → `LocalDateTime` qua
      `toInstant().atZone(ZoneId.systemDefault())`;
      `entry.getDescription().getValue()` để lấy text thật, có
      check null)
   4. Toàn bộ bọc `try/catch` — 1 feed lỗi không làm hỏng các nguồn
      khác
- `RssFetchScheduler` (package riêng `scheduler`) — giữ
  `Map<tên nguồn, URL feed>`, `@Scheduled(fixedRateString =
  "${rss.fetch.interval}")` chạy mỗi **6 giờ** (đọc từ
  `application.properties`, không hard-code), lặp gọi
  `RssFeedService` cho từng nguồn
- Bắt buộc thêm `@EnableScheduling` ở class `@SpringBootApplication`,
  nếu không mọi `@Scheduled` bị Spring ngó lơ hoàn toàn

### Danh sách bài báo (GET /api/articles) *(week 3)*

Dùng **cursor-based pagination** (không dùng trang số/`Pageable`
đơn giản) — chọn có chủ đích để phù hợp UX infinite-scroll, chấp
nhận tốn công hơn để tự viết logic.

1. `GET /api/articles?cursor=&size=` — `cursor` và `size` đều là
   tham số tùy chọn (`size` mặc định 10)
2. `cursor` = `id` của bài **cuối cùng** đã nhận ở lần gọi trước;
   không gửi (lần đầu) → Service dùng `Long.MAX_VALUE` nội bộ, coi
   như "lấy từ bài mới nhất"
3. Repository: `findByIdLessThanOrderByIdDesc(cursor, Pageable)` —
   `Pageable` chỉ dùng để giới hạn `size`, vị trí bắt đầu do điều
   kiện `WHERE id < cursor` quyết định (không phải offset)
4. Response (`ArticlePageResponse`): `articles`, `nextCursor` (id
   bài cuối lô, để gọi tiếp), `hasMore` (suy luận từ việc số bài trả
   về có bằng đúng `size` hay không, không cần query đếm riêng)

## Cấu trúc thư mục liên quan

```
com.langly.langly_backend
├── model/
│   ├── User.java
│   ├── AuthProvider.java              (LOCAL / GOOGLE / LINKED)
│   ├── PendingRegistration.java
│   ├── RefreshToken.java
│   ├── Article.java                   (mới)
│   └── SavedArticle.java              (mới)
├── repository/
│   ├── UserRepository.java
│   ├── PendingRegistrationRepository.java
│   ├── RefreshTokenRepository.java
│   ├── ArticleRepository.java         (mới)
│   └── SavedArticleRepository.java    (mới)
├── util/
│   ├── TokenGenerator.java            (static, SecureRandom)
│   └── JwtUtil.java                   (@Component, sinh/verify JWT)
├── service/
│   ├── EmailService.java
│   ├── AuthService.java               (register/login/refresh/logout + oauth2Login)
│   ├── SavedArticleService.java       (mới — toggle save/unsave)
│   ├── RssFeedService.java            (mới — đọc/parse/lọc/lưu 1 feed)
│   └── ArticleService.java            (mới — cursor pagination)
├── scheduler/                         (package mới)
│   └── RssFetchScheduler.java         (@Scheduled, mỗi 6h)
├── controller/
│   ├── AuthController.java            (+ logout)
│   ├── SavedArticleController.java    (mới)
│   └── ArticleController.java         (mới)
├── config/
│   ├── SecurityConfig.java
│   ├── PasswordEncoderConfig.java      
│   ├── JwtAuthenticationFilter.java
│   └── OAuth2SuccessHandler.java
├── dto/
│   ├── RegisterRequest.java / LoginRequest.java / RefreshTokenRequest.java
│   ├── AuthResponse.java
│   ├── ToggleSaveRequest.java / SavedStatusResponse.java     (mới)
│   └── ArticlePageResponse.java                              (mới)
└── exception/
    └── EmailAlreadyExistsException.java
```

## Cấu hình môi trường cần thiết

Đặt các biến sau qua environment variables (không hard-code vào
`application.properties`):

| Biến | Mô tả                                                  |
|---|--------------------------------------------------------|
| `MAIL_USERNAME` | Địa chỉ Gmail dùng để gửi mail xác nhận                |
| `MAIL_PASSWORD` | Gmail App Password (không phải mật khẩu Gmail thật)    |
| `JWT_SECRET` | Secret key ký JWT (chuỗi ngẫu nhiên ≥ 256-bit, Base64) |
| `GOOGLE_CLIENT_ID` | Client ID từ Google Cloud Console                      |
| `GOOGLE_CLIENT_SECRET` | Client Secret từ Google Cloud Console                  |

Cấu hình thường (không phải secret, đặt trong `application.properties`):

| Key | Mô tả | Giá trị hiện tại |
|---|---|---|
| `rss.fetch.interval` | Chu kỳ `RssFetchScheduler` chạy (ms) | `21600000` (6 giờ) |

## Ghi chú thiết kế đáng nhớ

- Access token không lưu DB (stateless) — verify chỉ dựa vào chữ ký +
  hạn dùng ghi sẵn trong token.
- Refresh token bắt buộc lưu DB — đây là cơ chế duy nhất cho phép
  server thu hồi phiên đăng nhập trước khi hết hạn tự nhiên.
- `AuthProvider` đã có sẵn `LINKED` để chuẩn bị cho Google OAuth2
  (Tuần 2): nếu Google trả về email trùng với tài khoản LOCAL đã có,
  hệ thống tự động hợp nhất 2 tài khoản.
- `Article` là dữ liệu dùng chung, không copy sang `SavedArticle` —
  `SavedArticle` chỉ giữ tham chiếu (`@ManyToOne`) nhẹ. Đã cân nhắc
  và loại bỏ phương án copy dữ liệu hoặc lưu bookmark phía client vì
  đi ngược mục tiêu đồng bộ đa thiết bị của hệ thống Auth.
- `RssFeedService` và `RssFetchScheduler` tách 2 class riêng (Single
  Responsibility) — Service không biết "khi nào/nguồn nào", chỉ biết
  "đọc 1 URL bất kỳ"; Scheduler giữ danh sách nguồn + lịch chạy.

