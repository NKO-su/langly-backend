# Langly Backend

## Overview
Đây là backend của một ứng dụng học ngoại ngữ thông qua các nội dung thực tế như tin tức hằng ngày.
Ứng dụng được định hướng hỗ trợ nhiều ngôn ngữ khác nhau thay vì chỉ tập trung vào tiếng Anh.
Backend chịu trách nhiệm cung cấp API và xử lý logic cho mobile app.

## Features
- Đăng ký tài khoản
- Đăng nhập
- Đăng nhập bằng Google
- Liên kết tài khoản
- Đăng xuất
- Bài báo
- Lưu bài báo
- Lấy bài báo từ RSS và tự động lưu
- Lấy danh sách bài báo

## Tech Stack
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- BCrypt / PasswordEncoder
- Spring Mail
- Spring Security OAuth2 Client
- Rome RSS

## Architecture
Backend được tổ chức thành các tầng, mỗi tầng thực hiện một chức năng riêng. 
Controller nhận request, Service xử lý logic và Repository truy cập database.

## Project Structure
```
com.langly.langly_backend
├── model/
│   ├── User.java
│   ├── AuthProvider.java
│   ├── PendingRegistration.java
│   ├── RefreshToken.java
│   ├── Article.java
│   └── SavedArticle.java       
├── repository/
│   ├── UserRepository.java
│   ├── PendingRegistrationRepository.java
│   ├── RefreshTokenRepository.java
│   ├── ArticleRepository.java  
│   └── SavedArticleRepository.java 
├── util/
│   ├── TokenGenerator.java      
│   └── JwtUtil.java               
├── service/
│   ├── EmailService.java
│   ├── AuthService.java          
│   ├── SavedArticleService.java      
│   ├── RssFeedService.java           
│   └── ArticleService.java          
├── scheduler/
│   └── RssFetchScheduler.java
├── controller/
│   ├── AuthController.java
│   ├── SavedArticleController.java
│   └── ArticleController.java
├── config/
│   ├── SecurityConfig.java
│   ├── PasswordEncoderConfig.java
│   ├── JwtAuthenticationFilter.java
│   └── OAuth2SuccessHandler.java
├── dto/
│   ├── RegisterRequest.java  
│   ├── LoginRequest.java  
│   ├── RefreshTokenRequest.java
│   ├── AuthResponse.java
│   ├── ToggleSaveRequest.java / SavedStatusResponse.java
│   └── ArticlePageResponse.java
└── exception/
    └── EmailAlreadyExistsException.java
```

## Authentication

### Đăng ký
- User đăng ký bằng email và password
- Gửi email xác nhận
- User xác nhận email
- Tạo tài khoản

### Đăng nhập
- User đăng nhập bằng email và password
- Kiểm tra thông tin đăng nhập
- Trả Access Token và Refresh Token

### Đăng nhập bằng Google
- Xác thực bằng Google
- Đăng nhập
- Trả Access Token và Refresh Token

### Liên kết tài khoản
- User đã đăng ký bằng email/password
- Đăng nhập bằng Google với cùng email
- Liên kết tài khoản Google với tài khoản hiện tại

### Đăng xuất
- Xóa Refresh Token

## Article & RSS System

### RSS
Backend lấy bài báo từ BBC RSS thông qua `RssFetchScheduler`.
Sau khi lấy được bài báo, backend kiểm tra xem bài báo đã tồn tại trong cơ sở dữ liệu hay chưa:
- Nếu chưa tồn tại, bài báo được lưu vào database.
- Nếu đã tồn tại, bài báo sẽ được bỏ qua.

### Article
Mobile gọi API của backend để lấy danh sách bài báo từ database. 
Backend trả về các bài báo để mobile hiển thị cho người dùng.

### Save Article
Khi user chọn lưu một bài báo, backend lấy `articleId` của bài báo và `userId` của user 
hiện tại, sau đó lưu mối quan hệ giữa hai ID này vào database.

## Configuration
Backend cần cấu hình các thành phần sau để có thể hoạt động:
- Kết nối với database
- Cấu hình mail để gửi email xác nhận tài khoản
- Cấu hình JWT secret key
- Cấu hình Google OAuth2
- Cấu hình thời gian tự động lấy bài báo từ RSS