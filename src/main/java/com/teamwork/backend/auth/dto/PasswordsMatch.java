package com.teamwork.backend.auth.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Mật khẩu xác nhận phải giống mật khẩu (UC01 kịch bản 4a). Lỗi được gắn vào trường {@code confirmPassword}
 * để frontend hiện ngay dưới ô "Xác nhận mật khẩu".
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

	String message() default "Mật khẩu xác nhận không khớp";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
