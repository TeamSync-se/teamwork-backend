package com.teamwork.backend.auth.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Độ dài mật khẩu theo {@code docs/api/auth.md}: từ 8 đến 64 ký tự, và không quá 72 byte khi mã hóa UTF-8
 * (giới hạn của BCrypt). Mỗi lần chỉ báo một lỗi, để frontend không hiện hai thông báo cùng nói "quá dài".
 * <p>
 * {@code null} được coi là hợp lệ; trường bắt buộc thì thêm {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = PasswordLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordLength {

	/** Không dùng: validator luôn trả một thông điệp cụ thể theo lỗi gặp phải. */
	String message() default "Độ dài mật khẩu không hợp lệ";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
