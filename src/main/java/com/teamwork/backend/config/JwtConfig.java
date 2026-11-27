package com.teamwork.backend.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Ký và kiểm tra token đăng nhập bằng HMAC SHA-256 với cùng một khóa bí mật: chỉ backend vừa phát hành vừa kiểm
 * tra token, nên không cần cặp khóa công khai, bí mật.
 * <p>
 * Có bean {@link JwtDecoder} thì Spring Boot không tạo tài khoản mặc định nữa, nên log khởi động không còn dòng
 * "Using generated security password".
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).algorithm(MacAlgorithm.HS256).build();
	}

	/**
	 * Từ chối token sai chữ ký, sai thuật toán hoặc đã hết hạn ({@code exp}).
	 * <p>
	 * Mặc định Spring vẫn nhận token quá hạn tới 60 giây, phòng khi đồng hồ của máy phát hành và máy kiểm tra
	 * lệch nhau. Ở đây cùng một backend vừa phát hành vừa kiểm tra, nên không cho lệch: token hết hạn đúng thời
	 * điểm {@code expiresAt} trả cho frontend (mục 2 của {@code docs/api/auth.md}).
	 */
	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		// Chỉ thay bước kiểm tra thời hạn; các bước kiểm tra mặc định khác của Spring vẫn giữ nguyên.
		decoder.setJwtValidator(
				JwtValidators.createDefaultWithValidators(List.of(new JwtTimestampValidator(Duration.ZERO))));
		return decoder;
	}

}
