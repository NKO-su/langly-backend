package com.langly.langly_backend.repository;

import com.langly.langly_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Note
 * extends JpaRepository<User, Long>    : Kế thừa JpaRepository,
 * + User   :Entity mà Repository này thao tác
 * + Long   :kiểu dữ liệu của khóa chính (id trong User là Long)
 *
 * Optional<User> findByEmail(String email) :findBy + Email  →  SELECT * FROM users WHERE email = ?
 * (Quy tắc này gọi là Query Method — Spring parse tên hàm theo cấu trúc findBy<TênField>, findBy<TênField>And<TênFieldKhác>)
 *  Optional<User> :buộc code gọi hàm này phải tường minh xử lý cả 2 trường hợp (có/không có), tránh lỗi NullPointerException.
 *
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
