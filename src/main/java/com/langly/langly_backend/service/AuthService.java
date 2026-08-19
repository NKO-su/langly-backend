package com.langly.langly_backend.service;

import com.langly.langly_backend.exception.EmailAlreadyExistsException;
import com.langly.langly_backend.model.AuthProvider;
import com.langly.langly_backend.model.PendingRegistration;
import com.langly.langly_backend.model.User;
import com.langly.langly_backend.repository.PendingRegistrationRepository;
import com.langly.langly_backend.repository.UserRepository;
import com.langly.langly_backend.util.TokenGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PendingRegistrationRepository pendingRegistrationRepository,EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.emailService = emailService;
    }

    public void register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email đã được đăng ký.");
        }

        Optional<PendingRegistration> existing = pendingRegistrationRepository.findByEmail(email);
        if (existing.isPresent()) {
            PendingRegistration pending = existing.get();
            if (!pending.isExpired()) {
                throw new IllegalStateException("Email này đang chờ xác nhận, vui lòng kiểm tra hộp thư.");
            }
            pendingRegistrationRepository.delete(pending);
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        String token = TokenGenerator.generateToken();
        PendingRegistration newPending = new PendingRegistration(email, hashedPassword, token);
        pendingRegistrationRepository.save(newPending);

        emailService.sendVerificationEmail(email, token);
    }
    /**
     * Note register
     *
     * 1. Kiểm tra Email đã tồn tại trong data chưa
     * 2. Kiểm tra Email có đang trong tình trạng chờ xác thực không.
     * -  Nếu có        -> thông báo
     * -  Nếu không     -> Xóa Record
     * 3. Bắt đầu hash Password
     * 4. Tạo token
     * 5. Tạo mới record tạm
     * 6. gửi mail xác nhận
     */

    public void resendVerification(String email) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy yêu cầu đăng ký nào cho email này."));

        if (pending.isExpired()) {
            throw new IllegalStateException("Yêu cầu đăng ký đã hết hạn, vui lòng đăng ký lại.");
        }
        if (pending.isInCooldown()) {
            throw new IllegalStateException("Vui lòng đợi ít nhất 60 giây giữa các lần gửi lại.");
        }
        if (pending.hasReachedResendLimit()) {
            throw new IllegalStateException("Đã đạt giới hạn gửi lại, vui lòng đăng ký lại sau.");
        }

        String newToken = TokenGenerator.generateToken();
        pending.regenerateToken(newToken);
        pendingRegistrationRepository.save(pending);

        emailService.sendVerificationEmail(email, newToken);
    }
    
    public void verifyEmail(String token) {
        PendingRegistration pending = pendingRegistrationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token không hợp lệ."));

        if (pending.isExpired()) {
            pendingRegistrationRepository.delete(pending);
            throw new IllegalStateException("Token đã hết hạn, vui lòng đăng ký lại.");
        }

        User newUser = new User(pending.getEmail(), pending.getPassword(), AuthProvider.LOCAL);
        userRepository.save(newUser);

        pendingRegistrationRepository.delete(pending);
    }
}