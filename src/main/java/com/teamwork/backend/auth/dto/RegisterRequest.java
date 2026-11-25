package com.teamwork.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đăng ký tài khoản, theo mục 1 của {@code docs/api/auth.md}.
 * <p>
 * Họ tên và email được bỏ khoảng trắng ở hai đầu trước khi kiểm tra. Mật khẩu giữ nguyên từng ký tự, kể cả
 * khoảng trắng.
 */
@PasswordsMatch
public record RegisterRequest(

		@NotBlank(message = "Họ tên không được để trống")
		@Size(max = 100, message = "Họ tên tối đa 100 ký tự")
		String fullName,

		@NotBlank(message = "Email không được để trống")
		@Email(message = "Email không đúng định dạng")
		@Size(max = 255, message = "Email tối đa 255 ký tự")
		String email,

		@NotNull(message = "Mật khẩu không được để trống")
		@PasswordLength
		String password,

		@NotNull(message = "Vui lòng nhập lại mật khẩu")
		String confirmPassword) {

	public RegisterRequest {
		fullName = (fullName != null) ? fullName.strip() : null;
		email = (email != null) ? email.strip() : null;
	}

}
