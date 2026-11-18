package com.teamwork.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Dữ liệu đăng nhập, theo mục 2 của {@code docs/api/auth.md}.
 * <p>
 * Email được bỏ khoảng trắng ở hai đầu. Mật khẩu giữ nguyên từng ký tự và chỉ cần không rỗng: đăng ký cho phép
 * mật khẩu gồm toàn khoảng trắng, nên không dùng {@code @NotBlank}.
 */
public record LoginRequest(

		@NotBlank(message = "Email không được để trống")
		@Email(message = "Email không đúng định dạng")
		String email,

		@NotEmpty(message = "Mật khẩu không được để trống")
		String password) {

	public LoginRequest {
		email = (email != null) ? email.strip() : null;
	}

}
