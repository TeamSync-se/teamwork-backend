package com.teamwork.backend.auth.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

	private static final int MIN_CHARS = 8;

	private static final int MAX_CHARS = 64;

	/**
	 * BCrypt ném lỗi với mật khẩu dài hơn 72 byte. Mật khẩu không dấu tối đa 64 byte nên không bao giờ chạm giới
	 * hạn này; một ký tự có dấu như "ệ" thì chiếm 3 byte.
	 */
	private static final int MAX_UTF8_BYTES = 72;

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		String message = errorMessage(password);
		if (message == null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
		return false;
	}

	/** Thông điệp của lỗi đầu tiên gặp phải, hoặc {@code null} nếu mật khẩu hợp lệ. */
	private static String errorMessage(String password) {
		if (password == null) {
			return null;
		}
		if (password.length() < MIN_CHARS) {
			return "Mật khẩu phải có ít nhất " + MIN_CHARS + " ký tự";
		}
		if (password.length() > MAX_CHARS) {
			return "Mật khẩu tối đa " + MAX_CHARS + " ký tự";
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
			return "Mật khẩu quá dài, vui lòng rút ngắn (ký tự có dấu được tính nhiều hơn một ký tự)";
		}
		return null;
	}

}
