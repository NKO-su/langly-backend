package com.langly.langly_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity

public class SecurityConfig {
    /**
     * Note
     * @Configuration               :báo Spring đây là class chứa các @Bean cần quản lý
     * @EnableWebSecurity: bật cơ chế bảo mật tùy chỉnh, thay cho cấu hình
     * mặc định của Spring Security (mặc định sẽ khóa TẤT CẢ endpoint và
     * sinh 1 password ngẫu nhiên - chính là dòng log WARN bạn từng thấy)
     */
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().permitAll()
            );
    return http.build();
}
}
