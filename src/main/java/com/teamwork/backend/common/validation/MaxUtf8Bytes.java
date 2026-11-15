package com.teamwork.backend.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Chuỗi khi mã hóa UTF-8 không được dài quá {@link #value()} byte; {@code null} được coi là hợp lệ.
 * <p>
 * Dùng cho mật khẩu: BCrypt chỉ nhận tối đa 72 byte và ném lỗi nếu dài hơn, trong khi một ký tự có dấu
 * như "ệ" chiếm 3 byte. Giới hạn theo số ký tự ({@code @Size}) vì thế chưa đủ.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {

	/** Số byte tối đa. */
	int value();

	String message() default "Dữ liệu quá dài";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
