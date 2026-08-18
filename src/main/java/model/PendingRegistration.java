package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.*;


/**
 * PendingRegistration — lưu tạm thông tin đăng ký local trước khi
 * user xác nhận email. KHÔNG phải User thật, chỉ tồn tại tối đa 30 phút.
 *
 * - Chỉ khi user bấm đúng link xác nhận (token khớp, chưa hết hạn),
 *   Service mới thật sự tạo User trong bảng users và xóa record này.
 * - Nếu user không xác nhận trong 30 phút, record này coi như "rác"
 *   hết hạn — có thể dọn bằng scheduled job sau này (chưa cần vội).
 * - Hỗ trợ gửi lại email xác nhận (resend), giới hạn: cooldown 60 giây
 *   giữa mỗi lần, tối đa 5 lần trong suốt vòng đời 30 phút của record.
 */
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

    /**
     * Kiểm tra còn trong thời gian cooldown 60 giây kể từ lần gửi trước không.
     */
    public boolean isInCooldown() {
        return LocalDateTime.now().isBefore(lastSentAt.plusSeconds(60));
    }

    /**
     * Kiểm tra đã dùng hết 5 lần gửi lại cho phép chưa.
     */
    public boolean hasReachedResendLimit() {
        return resendCount >= 5;
    }

    /**
     * Sinh token mới + reset hạn 30 phút + tăng đếm resend + cập nhật
     * thời điểm gửi gần nhất - gộp chung thành 1 hành động duy nhất,
     * tránh Service gọi rời rạc dễ quên 1 bước (ví dụ quên tăng resendCount).
     */
    public void regenerateToken(String newToken) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusMinutes(30);
        this.resendCount++;
        this.lastSentAt = LocalDateTime.now();
    }
}

