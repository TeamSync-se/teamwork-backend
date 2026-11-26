package com.teamwork.backend.user.dto;

import java.time.Instant;

import com.teamwork.backend.user.User;

/**
 * Thông tin tài khoản trả cho frontend, theo {@code docs/api/auth.md}. Không bao giờ chứa mật khẩu hay mã
 * băm mật khẩu.
 */
public record UserResponse(Long id, String fullName, String email, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
	}

}
