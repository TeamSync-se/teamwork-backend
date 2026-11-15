package com.teamwork.backend.auth;

import java.util.Locale;

import com.teamwork.backend.auth.dto.RegisterRequest;
import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.user.User;
import com.teamwork.backend.user.UserRepository;
import com.teamwork.backend.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Tạo tài khoản mới (UC01). Email được lưu ở dạng chữ thường, nên "A@Example.com" và "a@example.com" được
	 * coi là cùng một email. Mật khẩu chỉ được lưu dưới dạng mã băm BCrypt.
	 *
	 * @throws BusinessException với mã {@link AuthErrorCode#EMAIL_ALREADY_EXISTS} khi email đã được dùng
	 */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (this.userRepository.existsByEmail(email)) {
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}
		User user = new User(request.fullName(), email, this.passwordEncoder.encode(request.password()));
		try {
			user = this.userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Hai request cùng email đến gần như cùng lúc đều qua được bước kiểm tra ở trên; ràng buộc UNIQUE của
			// cột email chặn request đến sau. Đây là ràng buộc duy nhất có thể vi phạm vì mọi cột khác đều đã có giá trị.
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS,
					AuthErrorCode.EMAIL_ALREADY_EXISTS.defaultMessage(), ex);
		}
		return UserResponse.from(user);
	}

	private static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

}
