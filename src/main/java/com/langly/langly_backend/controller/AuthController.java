package com.langly.langly_backend.controller;

import com.langly.langly_backend.dto.RegisterRequest;
import com.langly.langly_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Đăng ký thành công, vui lòng kiểm tra email để xác nhận.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Xác nhận email thành công! Bạn có thể đăng nhập.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resend(@RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok("Đã gửi lại email xác nhận.");
    }
}
