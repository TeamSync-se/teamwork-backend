package com.teamwork.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.sql.SQLException;

import com.teamwork.backend.auth.dto.RegisterRequest;
import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.user.User;
import com.teamwork.backend.user.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Lỗi CSDL khi lưu tài khoản, khó tạo lại bằng request thật. Trường hợp chính: hai request cùng email đến gần
 * như cùng lúc, cả hai qua bước kiểm tra email, và ràng buộc UNIQUE của CSDL chặn request đến sau. Lỗi giả lập
 * có cùng dạng với lỗi thật mà {@code UserRepositoryTests} ghi nhận với CSDL thật.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	private static final RegisterRequest REQUEST = new RegisterRequest("Nguyễn Văn A", "A@Example.com", "matkhau123",
			"matkhau123");

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void emailNotYetTaken() {
		// Email được kiểm tra ở dạng chữ thường
		given(this.userRepository.existsByEmail("a@example.com")).willReturn(false);
		given(this.passwordEncoder.encode("matkhau123")).willReturn("$2a$10$hash");
	}

	@Test
	void concurrentRegistrationWithSameEmailIsRejectedAsEmailAlreadyExists() {
		given(this.userRepository.saveAndFlush(any(User.class))).willThrow(constraintViolation("users_email_key"));

		assertThatThrownBy(() -> this.authService.register(REQUEST)).isInstanceOfSatisfying(BusinessException.class,
				(ex) -> {
					assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);
					assertThat(ex.getMessage()).isEqualTo("Email đã được sử dụng");
				});
	}

	@Test
	void otherConstraintViolationIsNotReportedAsDuplicateEmail() {
		DataIntegrityViolationException error = constraintViolation("users_some_other_key");
		given(this.userRepository.saveAndFlush(any(User.class))).willThrow(error);

		assertThatThrownBy(() -> this.authService.register(REQUEST)).isSameAs(error);
	}

	@Test
	void otherDatabaseErrorIsNotReportedAsDuplicateEmail() {
		DataIntegrityViolationException error = new DataIntegrityViolationException(
				"value too long for type character varying(255)");
		given(this.userRepository.saveAndFlush(any(User.class))).willThrow(error);

		assertThatThrownBy(() -> this.authService.register(REQUEST)).isSameAs(error);
	}

	private static DataIntegrityViolationException constraintViolation(String constraintName) {
		SQLException sqlError = new SQLException("duplicate key value violates unique constraint", "23505");
		return new DataIntegrityViolationException("could not execute statement",
				new ConstraintViolationException("could not execute statement", sqlError, constraintName));
	}

}
