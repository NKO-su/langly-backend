package com.langly.langly_backend.service;

import com.langly.langly_backend.dto.AuthResponse;
import com.langly.langly_backend.exception.EmailAlreadyExistsException;
import com.langly.langly_backend.model.AuthProvider;
import com.langly.langly_backend.model.PendingRegistration;
import com.langly.langly_backend.model.RefreshToken;
import com.langly.langly_backend.model.User;
import com.langly.langly_backend.repository.PendingRegistrationRepository;
import com.langly.langly_backend.repository.RefreshTokenRepository;
import com.langly.langly_backend.repository.UserRepository;
import com.langly.langly_backend.util.JwtUtil;
import com.langly.langly_backend.util.TokenGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       PendingRegistrationRepository pendingRegistrationRepository, EmailService emailService, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
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
    public void register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email đã được đăng ký.");
        }

        Optional<PendingRegistration> existing = pendingRegistrationRepository.findByEmail(email);
        if (existing.isPresent()) {
            PendingRegistration pending = existing.get();
            if (!pending.isExpired()) {                 //.isExpired()  : Het han
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
     * Note resendVerification
     * - Kiểm tra email có tồn tại trong pendingRegistrationRepository
     *
     *  Kiểm tra Expired,isInCooldown,hasReachedResendLimit của email
     *
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

    /**
     * verifyEmail Note
     *
     * - Lấy user có token trùng
     * - Kiểm tra hạn token
     * - Nếu hết hạn - xóa khỏi DB
     * - Nếu không, tạo user mới - dữ liệu lấy từ pendingRegistration
     * - xóa pendingRegistration khi tọa xong
     */
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

    public AuthResponse login(String email, String rawPassword){
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalStateException("Email hoặc Password không hơp lệ."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalStateException("Email hoặc Password không hơp lệ.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshtoken= jwtUtil.generateRefreshTokenValue(user.getEmail());

        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000);

        RefreshToken newRefreshToken = new RefreshToken(refreshtoken,user,refreshTokenExpiresAt);
        refreshTokenRepository.save(newRefreshToken);


        return new AuthResponse(accessToken,refreshtoken);
    }

    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalStateException("Refresh token không hợp lệ."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalStateException("Refresh token đã hết hạn, vui lòng đăng nhập lại.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());

        return new AuthResponse(newAccessToken, refreshTokenValue);
    }

    public AuthResponse oauth2Login(String email){
        Optional<User> googleUser = userRepository.findByEmail(email);

        User user;

        if (googleUser.isEmpty()) {
            user = new User(email,null, AuthProvider.GOOGLE);
            userRepository.save(user);
        } else {
            user = googleUser.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.LINKED);
                userRepository.save(user);
            }
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshtoken= jwtUtil.generateRefreshTokenValue(user.getEmail());

        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration() / 1000);

        RefreshToken newRefreshToken = new RefreshToken(refreshtoken,user,refreshTokenExpiresAt);
        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(accessToken,refreshtoken);
    }

    public void logout(String refreshTokenValue) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(refreshTokenValue);
        if (refreshToken.isPresent()) {
            refreshTokenRepository.delete(refreshToken.get());
        }
    }
}