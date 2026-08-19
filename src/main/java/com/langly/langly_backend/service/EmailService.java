package com.langly.langly_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Note
 *
 * - JavaMailSender           : interface có sẵn của Spring, tự động cấu hình
 * dựa theo spring.mail.* trong application.properties. Không cần
 * tự viết code kết nối SMTP thủ công.
 *
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = "http://localhost:8080/api/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác nhận đăng ký tài khoản Langly");
        message.setText("Chào bạn,\n\nVui lòng bấm vào link sau để xác nhận đăng ký (link có hiệu lực trong 30 phút):\n"
                + verificationLink
                + "\n\nNếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email.");

        mailSender.send(message);
    }
}