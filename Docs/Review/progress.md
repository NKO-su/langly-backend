# Backend Review Progress

## 2026-09-14 — Architecture Review

### Đã review

* Controller và Service
* DTO và `@Valid` / `@RequestBody`
* Dependency Injection
* Constructor Injection
* `final` dependency field
* `UserRepository`
* `JpaRepository<User, Long>`
* Query Method
* `Optional<User>`
* `findById()` và `findByEmail()`
* `findBy...` và `existsBy...`

### Những hiểu nhầm đã được sửa

* DTO không phải chủ yếu để bảo mật; DTO dùng để đóng gói dữ liệu request và kết hợp với validation.
* `Long` trong `JpaRepository<User, Long>` là kiểu dữ liệu của primary key, không phải bản thân ID.
* `Optional` không tìm dữ liệu; Repository thực hiện việc truy vấn và `Optional` biểu diễn kết quả có hoặc không có dữ liệu.
* `findById()` là method được kế thừa từ `JpaRepository`.
* `findByEmail()` là method do project khai báo; Spring Data JPA xử lý method này theo Query Method Convention.
* `existsByUsername()` phù hợp hơn `findByUsername()` khi chỉ cần biết dữ liệu có tồn tại hay không.
* `AuthService` không tự tạo `UserRepository`; Spring Data JPA tạo implementation/proxy cho Repository và Spring inject dependency đó vào `AuthService`.
* `this.userRepository` là field của `AuthService`, còn `userRepository` trong constructor là parameter; hai biến có thể cùng tham chiếu đến một object.

### Đã hiểu được ở mức cơ bản

* Controller chịu trách nhiệm giao tiếp HTTP với Client.
* Service chịu trách nhiệm xử lý business logic và điều phối các dependency cần thiết.
* Repository chịu trách nhiệm truy cập dữ liệu.
* Dependency Injection giúp class sử dụng dependency mà không phải tự tạo và quản lý dependency đó.
* `JpaRepository` cung cấp các thao tác repository có sẵn như `save()`, `findById()`, `findAll()`, `delete()`.
* Spring Data JPA có thể xử lý các Query Method dựa trên quy ước tên method.

### Chưa review xong

* Request flow hoàn chỉnh từ Client → Controller → Service → Repository → Database → Response.
* Spring Bean và quá trình khởi tạo dependency ở mức sâu hơn.
* Database interaction thực tế của JPA/Hibernate.
* Rebuild: khả năng tự xây dựng lại các thành phần mà không nhìn code gốc.
* Design: khả năng tự đưa ra quyết định kiến trúc tương tự trong một yêu cầu mới.

### Trạng thái

Explain: Đã nắm phần lớn nội dung đã review.

Reason: Đã hiểu phần lớn các quyết định đã được hỏi.

Rebuild: Chưa kiểm tra đầy đủ.

Design: Đã kiểm tra một số trường hợp đơn giản, chưa hoàn tất.

### Next Review

Tiếp tục Architecture Review với request flow và mối quan hệ:

Client
→ Controller
→ DTO / Validation
→ Service
→ Repository
→ Database
→ Response

## 2026-09-15

### Đã review

#### Architecture

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

#### Local Registration

* `PendingRegistration` và lý do không tạo `User` ngay
* Hash password trước khi lưu pending data
* Verification token
* Xử lý `PendingRegistration` hết hạn
* Lưu pending registration trước khi gửi email
* Resend verification và tạo token mới
* Verify email → tạo `User`
* Xóa `PendingRegistration` sau khi verification hoàn tất
* Transaction và tính nhất quán dữ liệu ở mức cơ bản

#### Login / Token

* `PasswordEncoder.matches()`
* Không phân biệt lỗi "email không tồn tại" và "sai password"
* Access Token và Refresh Token
* Access Token 1 giờ, Refresh Token 6 tháng
* Access Token không lưu DB
* Refresh Token lưu DB để có thể chủ động revoke
* Refresh Access Token
* Hiểu vì sao hiện tại không dùng refresh-token rotation
* Logout bằng cách xóa Refresh Token
* Trade-off của việc không blacklist Access Token

### Những điểm đã sửa trong quá trình review

* DTO không chủ yếu để bảo mật; DTO đóng gói dữ liệu request và kết hợp validation.
* `Long` trong `JpaRepository<User, Long>` là kiểu dữ liệu của ID.
* `Optional` không thực hiện việc tìm dữ liệu; Repository thực hiện truy vấn và `Optional` biểu diễn kết quả có/không có dữ liệu.
* `findById()` được kế thừa từ `JpaRepository`.
* `findByEmail()` do project khai báo; Spring Data JPA xử lý theo Query Method Convention.
* `AuthService` không tự tạo `UserRepository`; Spring Data JPA tạo implementation/proxy và Spring inject dependency.
* `this.userRepository` là field của `AuthService`, còn parameter `userRepository` trong constructor là biến được truyền vào.
* Logout không làm Access Token mất hiệu lực ngay lập tức; token hiện tại vẫn có thể dùng cho đến khi hết hạn.

### Chưa review

* `JwtAuthenticationFilter`
* `SecurityConfig`
* Spring Security Filter Chain
* `SecurityContext`
* Cách Access Token được dùng để xác thực các request sau login
* Transaction ở mức implementation
* Rebuild Architecture từ đầu mà không nhìn code

### Trạng thái

Architecture:

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra đầy đủ
* Design: ✓ ở mức cơ bản

Local Registration:

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra
* Design: ✓

Login / Token:

* Explain: ✓
* Reason: ✓
* Rebuild: chưa kiểm tra đầy đủ
* Design: ✓ ở mức hiện tại

### Next Review

Tiếp tục với:

`JwtAuthenticationFilter`
→ `SecurityConfig`
→ Filter Chain
→ `SecurityContext`
