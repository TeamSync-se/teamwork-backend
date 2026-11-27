package com.teamwork.backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	/** {@code email} phải đã được chuẩn hóa về chữ thường, vì CSDL so sánh có phân biệt hoa thường. */
	boolean existsByEmail(String email);

	/** {@code email} phải đã được chuẩn hóa về chữ thường, như {@link #existsByEmail(String)}. */
	Optional<User> findByEmail(String email);

}
