package com.teamwork.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Cấu hình Spring Security chung. Các issue sau mở rộng lớp này: đăng nhập JWT (#9) thêm bộ lọc token,
 * mở {@code /api/auth/**} và trả lỗi 401, 403 theo định dạng lỗi chung.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	private static final String REGISTER_PATH = "/api/auth/register";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Bộ lọc CORS đứng trước bước xác thực, nên request kiểm tra trước (preflight) không cần token
			// và response 401 vẫn có header CORS để frontend đọc được mã lỗi.
			.cors(Customizer.withDefaults())
			// Khách chưa có tài khoản nên không có token CSRF. #9 sẽ tắt CSRF cho mọi API vì backend không dùng
			// cookie phiên.
			.csrf((csrf) -> csrf.ignoringRequestMatchers(REGISTER_PATH))
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
				// Mở mọi phương thức, không chỉ POST: gọi sai phương thức thì nhận 405 thay vì 401 (quy ước API mục 4)
				.requestMatchers(REGISTER_PATH).permitAll()
				.anyRequest().authenticated())
			// Không khai báo thì Spring Security trả 403 cho request chưa đăng nhập; quy ước API là 401.
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
		return http.build();
	}

	/** Băm mật khẩu bằng BCrypt (NFR-01, QĐ01). BCrypt chỉ nhận tối đa 72 byte; xem {@code @PasswordLength}. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
