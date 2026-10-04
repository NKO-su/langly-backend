package com.langly.langly_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int resendCount;

    @Column(nullable = false)
    private LocalDateTime lastSentAt;

    protected PendingRegistration() {
    }

    public PendingRegistration(String email, String password, String token) {
        this.email = email;
        this.password = password;
        this.token = token;
        this.expiresAt = LocalDateTime.now().plusMinutes(30);
        this.resendCount = 0;
        this.lastSentAt = LocalDateTime.now();
    }

    public void regenerateToken(String newToken) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusMinutes(30);
        this.resendCount++;
        this.lastSentAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getResendCount() {
        return resendCount;
    }

    public LocalDateTime getLastSentAt() {
        return lastSentAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    public boolean isInCooldown() {
        return LocalDateTime.now().isBefore(lastSentAt.plusSeconds(60));
    }
    public boolean hasReachedResendLimit() {
        return resendCount >= 5;
    }
}

