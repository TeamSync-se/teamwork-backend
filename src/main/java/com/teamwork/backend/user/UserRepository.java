package com.teamwork.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	/** {@code email} phải đã được chuẩn hóa về chữ thường, vì CSDL so sánh có phân biệt hoa thường. */
	boolean existsByEmail(String email);

}
