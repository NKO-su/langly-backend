package com.langly.langly_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    /**
     * Note
     * @Entity                  :Đại diện cho 1 bảng trong Database
     * @Table(name = "users")   :Chỉ định rõ tên bảng trong Database
     * @Id                      : Đánh dấu Private Key
     *
     * GeneratedValue(strategy = GenerationType.IDENTITY):Tự động tăng giá trị
     * @Column(unique = true, nullable = false) :Cấu hình ràng buộc
     * @Enumerated(EnumType.STRING): bắt buộc phải có khi field là kiểu enum
     * updatable = false        :không cho phép update
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = true)
    private String providerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User(){}
    public User(String email, String password, AuthProvider provider) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
