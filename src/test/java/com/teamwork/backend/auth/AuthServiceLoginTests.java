package com.teamwork.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import com.teamwork.backend.auth.dto.LoginRequest;
import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Những điều {@code LoginTests} không quan sát được qua API: email không tồn tại vẫn tốn thời gian so mật khẩu
 * như email có thật.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTests {

	private static final String DUMMY_HASH = "$2a$10$dummy";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenService jwtTokenService;

	private AuthService authService;

	@BeforeEach
	void createService() {
		given(this.passwordEncoder.encode(anyString())).willReturn(DUMMY_HASH);
		this.authService = new AuthService(this.userRepository, this.passwordEncoder, this.jwtTokenService);
	}

	@Test
	void unknownEmailStillComparesPasswordAgainstDummyHash() {
		given(this.userRepository.findByEmail("a@example.com")).willReturn(Optional.empty());

		assertThatThrownBy(() -> this.authService.login(new LoginRequest(" A@Example.com ", "matkhau123")))
			.isInstanceOfSatisfying(BusinessException.class,
					(ex) -> assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
		then(this.passwordEncoder).should().matches("matkhau123", DUMMY_HASH);
		verifyNoInteractions(this.jwtTokenService);
	}

	@Test
	void passwordLongerThan72BytesIsRejectedBeforeLookingUpEmail() {
		assertThatThrownBy(() -> this.authService.login(new LoginRequest("a@example.com", "ệ".repeat(24) + "x")))
			.isInstanceOfSatisfying(BusinessException.class,
					(ex) -> assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
		verifyNoInteractions(this.userRepository, this.jwtTokenService);
	}

}
