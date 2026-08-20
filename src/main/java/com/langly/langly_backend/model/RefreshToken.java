package com.langly.langly_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User  user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected  RefreshToken() {}

    public RefreshToken(String token, User  user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public User  getUser() {
        return user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
