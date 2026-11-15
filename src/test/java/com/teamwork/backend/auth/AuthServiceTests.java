package com.teamwork.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.teamwork.backend.auth.dto.RegisterRequest;
import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.user.User;
import com.teamwork.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Trường hợp khó tạo lại bằng request thật: hai request cùng email đến gần như cùng lúc, cả hai qua bước kiểm
 * tra email, và ràng buộc UNIQUE của CSDL chặn request đến sau.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService authService;

	@Test
	void concurrentRegistrationWithSameEmailIsRejectedAsEmailAlreadyExists() {
		// Email được kiểm tra ở dạng chữ thường
		given(this.userRepository.existsByEmail("a@example.com")).willReturn(false);
		given(this.passwordEncoder.encode("matkhau123")).willReturn("$2a$10$hash");
		given(this.userRepository.saveAndFlush(any(User.class)))
			.willThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

		RegisterRequest request = new RegisterRequest("Nguyễn Văn A", "A@Example.com", "matkhau123", "matkhau123");

		assertThatThrownBy(() -> this.authService.register(request)).isInstanceOfSatisfying(BusinessException.class,
				(ex) -> {
					assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);
					assertThat(ex.getMessage()).isEqualTo("Email đã được sử dụng");
				});
	}

}
