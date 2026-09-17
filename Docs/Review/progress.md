# Backend Review Progress

## 2026-09-14 — Architecture Review

### Đã review

* Controller và Service
* DTO, `@RequestBody`, `@Valid`
* Dependency Injection
* Constructor Injection
* `final` dependency
* Repository
* `JpaRepository<User, Long>`
* Spring Data JPA Query Method
* `findById()`
* `findByEmail()`
* `existsBy...()`
* `Optional`
* Request flow cơ bản:
  Client → Controller → DTO/Validation → Service → Repository/Dependencies → Database/Email → Controller → Client

### Những hiểu nhầm đã được sửa

* DTO không chủ yếu để bảo mật; DTO đóng gói dữ liệu request và kết hợp validation.
* `Long` trong `JpaRepository<User, Long>` là kiểu dữ liệu của ID.
* `Optional` không thực hiện việc tìm dữ liệu; Repository thực hiện truy vấn và `Optional` biểu diễn kết quả có/không có dữ liệu.
* `findById()` được kế thừa từ `JpaRepository`.
* `findByEmail()` do project khai báo; Spring Data JPA xử lý theo Query Method Convention.
* `AuthService` không tự tạo `UserRepository`; Spring Data JPA tạo implementation/proxy và Spring inject dependency.
* `this.userRepository` là field của `AuthService`, còn parameter `userRepository` trong constructor là biến được truyền vào.

---

## 2026-09-15 — Local Registration & JWT Authentication Review

### Đã review

#### Local Registration

* `PendingRegistration` và lý do không tạo `User` ngay.
* Hash password trước khi lưu pending data.
* Verification token.
* Xử lý `PendingRegistration` hết hạn.
* Lưu pending registration trước khi gửi email.
* Resend verification và tạo token mới.
* Verify email → tạo `User`.
* Xóa `PendingRegistration` sau khi verification hoàn tất.
* Transaction và tính nhất quán dữ liệu ở mức cơ bản.

#### Login / Token

* `PasswordEncoder.matches()`.
* Không phân biệt lỗi "email không tồn tại" và "sai password".
* Access Token và Refresh Token.
* Access Token 1 giờ, Refresh Token 6 tháng.
* Access Token không lưu DB.
* Refresh Token lưu DB để server có thể chủ động revoke.
* Refresh Access Token.
* Không sử dụng refresh-token rotation trong thiết kế hiện tại.
* Logout bằng cách xóa Refresh Token.
* Access Token không bị blacklist khi logout.
* Access Token hiện tại vẫn có thể được sử dụng cho tới khi hết hạn sau logout.

### Những hiểu nhầm đã được sửa

* Password hash không phải encryption để có thể decrypt lại.
* `PasswordEncoder.matches()` dùng để kiểm tra raw password với password hash đã lưu.
* Refresh Token không chứa Access Token; nó được dùng để xin Access Token mới.
* Access Token không phải "token khi Refresh Token còn hiệu lực"; hai token có thời hạn và mục đích khác nhau.
* Logout không làm Access Token mất hiệu lực ngay lập tức.
* Refresh Token được lưu DB để server có thể kiểm tra và revoke phiên đăng nhập.

---

## 2026-09-17 — Spring Security Authentication Flow Review

### Đã review

* Vai trò của `JwtAuthenticationFilter`.
* Tại sao không để từng Controller tự xác minh JWT.
* Access Token được gửi trong HTTP header:
  `Authorization: Bearer <token>`.
* `JwtAuthenticationFilter` xác minh Access Token trước khi request đi vào protected API.
* `SecurityContext` giữ kết quả authentication của request hiện tại.
* `Authentication` đại diện cho user đã được xác thực.
* Phân biệt authentication với business logic.
* Service có thể sử dụng thông tin user hiện tại để xử lý dữ liệu đúng user.
* `permitAll()` cho `/api/auth/**`.
* `.authenticated()` cho các API protected.
* Lý do login/register phải public: client chưa có Access Token để xác thực trước đó.
* `JwtAuthenticationFilter` phải chạy đủ sớm để thiết lập authentication trước khi Spring Security kiểm tra quyền truy cập.
* Luồng xác thực:
  Client → Access Token → JwtAuthenticationFilter → SecurityContext → Controller → Service → Repository.
* Khi Access Token hết hạn, Filter không tự refresh token.
* Client chịu trách nhiệm gọi `/api/auth/refresh` bằng Refresh Token.
* Server kiểm tra Refresh Token rồi cấp Access Token mới.
* Khi cả Access Token và Refresh Token đều hết hạn, phiên đăng nhập phải kết thúc và user cần đăng nhập lại.
* Từ `SecurityContext`, phần phía sau có thể biết request hiện tại thuộc về user nào.
* Không nên tin một `userId` do client tự gửi để quyết định user nào được truy cập dữ liệu riêng; server nên dựa vào user đã được authentication xác định.

### Những hiểu nhầm đã được sửa

* `SecurityContext` không phải nơi xin Access Token mới; nó giữ thông tin authentication hiện tại.
* Service không dùng `SecurityContext` để quyết định "có xin Access Token hay không"; authentication và business logic là hai trách nhiệm khác nhau.
* Access Token hết hạn không khiến `JwtAuthenticationFilter` tự gọi refresh.
* Refresh Token được client giữ và gửi tới `/api/auth/refresh` khi cần.
* Access Token dùng để gọi API protected; Refresh Token dùng để xin Access Token mới.
* Authentication không phải để "biết trả response về cho user nào"; HTTP response đã quay lại client gửi request. Thông tin user dùng để xác định dữ liệu nào được phép truy cập hoặc thay đổi.

### Trạng thái hiện tại

#### Architecture

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra đầy đủ
* Design: ✓ ở mức cơ bản

#### Local Registration

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra
* Design: ✓

#### Login / Token

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra đầy đủ
* Design: ✓ ở mức hiện tại

#### Spring Security Authentication

* Explain: 
* Reason: 
* Rebuild: chưa kiểm tra
* Design: đang hoàn thiện

### Chưa review

* Code thực tế của `JwtAuthenticationFilter`.
* `SecurityFilterChain` và `SecurityConfig` ở mức implementation.
* `Authentication` / `Principal` trong code thực tế.
* Cách lấy user hiện tại từ `SecurityContext`.
* Cách Android/Retrofit/OkHttp tự động xử lý Access Token hết hạn và refresh.

### Next Review

Đọc và review trực tiếp code:

`JwtAuthenticationFilter`
→ `SecurityFilterChain`
→ `SecurityContext`
→ lấy current user
→ protected API


✓