package com.teamwork.backend.auth.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegisterRequest> {

	/** Khớp tên trường JSON mà frontend gửi lên. */
	private static final String CONFIRM_PASSWORD_FIELD = "confirmPassword";

	@Override
	public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
		// Thiếu một trong hai trường thì ràng buộc @NotNull của trường đó đã báo lỗi; không báo thêm.
		if (request == null || request.password() == null || request.confirmPassword() == null
				|| request.password().equals(request.confirmPassword())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
			.addPropertyNode(CONFIRM_PASSWORD_FIELD)
			.addConstraintViolation();
		return false;
	}

}
