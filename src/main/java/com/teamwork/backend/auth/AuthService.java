package com.teamwork.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.teamwork.backend.auth.dto.LoginRequest;
import com.teamwork.backend.auth.dto.LoginResponse;
import com.teamwork.backend.auth.dto.RegisterRequest;
import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.user.User;
import com.teamwork.backend.user.UserRepository;
import com.teamwork.backend.user.dto.UserResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	/** Giới hạn của BCrypt; {@code @PasswordLength} cũng dùng giới hạn này khi đăng ký. */
	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenService jwtTokenService;

	/**
	 * Mã băm của một mật khẩu ngẫu nhiên, dùng để so khi email không tồn tại. BCrypt cố ý chạy chậm; nếu bỏ qua
	 * bước so thì đăng nhập bằng email không tồn tại trả lời nhanh hơn hẳn, và người ngoài đo thời gian là biết
	 * email nào đã có tài khoản.
	 */
	private final String dummyPasswordHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
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
			// cột email chặn request đến sau. Lỗi CSDL khác vẫn ném ra để thành lỗi 500, không bị báo nhầm là
			// trùng email.
			if (!violatesEmailUniqueConstraint(ex)) {
				throw ex;
			}
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS,
					AuthErrorCode.EMAIL_ALREADY_EXISTS.defaultMessage(), ex);
		}
		return UserResponse.from(user);
	}

	/**
	 * Đăng nhập (UC02), trả access token. Email không phân biệt chữ hoa, chữ thường.
	 *
	 * @throws BusinessException với mã {@link AuthErrorCode#INVALID_CREDENTIALS} khi sai email hoặc sai mật khẩu
	 * (UC02 kịch bản 4a); hai trường hợp không phân biệt được với nhau
	 */
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		// BCrypt chỉ so 72 byte đầu, nên nếu không chặn thì "mật khẩu đúng + ký tự thừa" cũng khớp. Mật khẩu dài
		// hơn 72 byte không thể là mật khẩu đúng vì đăng ký đã từ chối. Kiểm tra này không phụ thuộc email nên
		// không lộ email có tồn tại hay không.
		if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		Optional<User> user = this.userRepository.findByEmail(normalizeEmail(request.email()));
		String passwordHash = user.map(User::getPasswordHash).orElse(this.dummyPasswordHash);
		boolean passwordMatches = this.passwordEncoder.matches(request.password(), passwordHash);
		if (user.isEmpty() || !passwordMatches) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		Jwt token = this.jwtTokenService.issueAccessToken(user.get());
		return LoginResponse.bearer(token.getTokenValue(), token.getExpiresAt());
	}

	private static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	/**
	 * Spring chỉ trả {@link DataIntegrityViolationException} chung cho mọi lỗi ràng buộc; tên ràng buộc bị vi
	 * phạm nằm trong lỗi gốc của Hibernate ({@code UserRepositoryTests} kiểm tra điều này với CSDL thật).
	 */
	private static boolean violatesEmailUniqueConstraint(DataIntegrityViolationException ex) {
		return ex.getCause() instanceof ConstraintViolationException violation
				&& User.EMAIL_UNIQUE_CONSTRAINT.equals(violation.getConstraintName());
	}

}
