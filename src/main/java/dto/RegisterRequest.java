package dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Note
 *
 * - Tách riêng khỏi User entity vì client chỉ được phép gửi email + password
 *   → không cho client tự set provider, providerId, id... (tránh giả mạo dữ liệu)
 *
 * - Các annotation @NotBlank, @Email, @Size là validation, tự động chạy
 *   TRƯỚC KHI vào tới Controller, nếu sai sẽ tự trả lỗi 400 Bad Request
 *   (cần thêm @Valid ở tham số Controller mới kích hoạt — sẽ làm ở bước Controller)
 */
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8, message = "Password phải có ít nhất 8 ký tự")
    private String password;

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
}
