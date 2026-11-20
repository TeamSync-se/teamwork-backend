package com.teamwork.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cho phép frontend chạy ở origin khác (ví dụ Next.js ở {@code http://localhost:3000}) gọi API từ trình
 * duyệt. Spring Security áp cấu hình này qua {@code http.cors(...)} trong {@link SecurityConfig}, nên
 * header CORS có cả trên response bị từ chối vì chưa đăng nhập.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

	private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	private static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");

	/**
	 * Tên bean phải là {@code corsConfigurationSource}: Spring Security tìm bean theo tên này khi bật CORS.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(ALLOWED_METHODS);
		configuration.setAllowedHeaders(ALLOWED_HEADERS);
		// Backend không dùng cookie; header Authorization do frontend tự gắn nên không cần bật credentials.
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
