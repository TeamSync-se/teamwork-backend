package com.teamwork.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cấu hình Spring Security chung (mục 3 của {@code docs/api/conventions.md}): mọi API, trừ
 * {@code /api/auth/**} và {@code /api/health}, phải gửi header {@code Authorization: Bearer <token>}. Token do
 * {@code POST /api/auth/login} phát hành; khóa ký nằm ở {@link JwtConfig}.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	/**
	 * API không cần đăng nhập. Mở cho mọi phương thức, không chỉ phương thức API thật sự nhận: gọi sai phương thức
	 * thì nhận 405 thay vì 401 (quy ước API mục 4).
	 */
	private static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
			PathPatternRequestMatcher.pathPattern("/api/auth/**"), PathPatternRequestMatcher.pathPattern("/api/health"));

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
		SecurityErrorHandler errorHandler = new SecurityErrorHandler(jsonMapper);
		http
			// Bộ lọc CORS đứng trước bước xác thực, nên request kiểm tra trước (preflight) không cần token
			// và response 401 vẫn có header CORS để frontend đọc được mã lỗi.
			.cors(Customizer.withDefaults())
			// Token đi trong header, không dùng cookie phiên, nên không cần CSRF và không tạo session.
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// Backend không có API đăng xuất: frontend tự xóa token (docs/api/auth.md mục 4).
			.logout(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer((resourceServer) -> resourceServer
				.jwt(Customizer.withDefaults())
				.bearerTokenResolver(bearerTokenResolver())
				.authenticationEntryPoint(errorHandler)
				.accessDeniedHandler(errorHandler))
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint(errorHandler)
				.accessDeniedHandler(errorHandler));
		return http.build();
	}

	/**
	 * Bỏ qua header {@code Authorization} ở API công khai. Nếu không, token cũ hoặc hỏng còn nằm trong cookie của
	 * frontend làm chính API đăng nhập trả 401, và người dùng không đăng nhập lại được.
	 */
	private static BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
		return (request) -> PUBLIC_ENDPOINTS.matches(request) ? null : headerResolver.resolve(request);
	}

	/** Băm mật khẩu bằng BCrypt (NFR-01, QĐ01). BCrypt chỉ nhận tối đa 72 byte; xem {@code @PasswordLength}. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
