package com.teamwork.backend.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
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
	 * Từ chối token sai chữ ký, sai thuật toán, thiếu {@code exp} hoặc đã hết hạn, và token có {@code sub} không
	 * phải id người dùng.
	 * <p>
	 * Mặc định Spring vẫn nhận token quá hạn tới 60 giây, phòng khi đồng hồ của máy phát hành và máy kiểm tra
	 * lệch nhau. Ở đây cùng một backend vừa phát hành vừa kiểm tra, nên không cho lệch: token hết hạn đúng thời
	 * điểm {@code expiresAt} trả cho frontend (mục 2 của {@code docs/api/auth.md}).
	 * <p>
	 * {@code JwtTokenService} luôn ghi đủ {@code exp} và {@code sub}, nhưng vẫn kiểm tra lại ở đây, không dựa vào
	 * giả định token nào cũng do backend tạo. Mặc định Spring nhận token thiếu {@code exp} (không bao giờ hết hạn),
	 * và {@code sub} không phải số sẽ gây lỗi 500 khi {@code @CurrentUserId} đổi nó sang số. Kiểm tra ở bước xác
	 * thực thì các token này bị trả 401 như mọi token sai khác.
	 */
	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		JwtTimestampValidator expiry = new JwtTimestampValidator(Duration.ZERO);
		expiry.setAllowEmptyExpiryClaim(false);
		JwtClaimValidator<String> subject = new JwtClaimValidator<>(JwtClaimNames.SUB, JwtConfig::isUserId);
		// Thay bước kiểm tra thời hạn và thêm bước kiểm tra sub; các bước kiểm tra mặc định khác của Spring vẫn
		// giữ nguyên.
		decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(expiry, subject)));
		return decoder;
	}

	/** {@code sub} là id người dùng: số nguyên dương nằm trong kiểu {@code Long}, đúng như {@code @CurrentUserId} cần. */
	private static boolean isUserId(String subject) {
		if (subject == null) {
			return false;
		}
		try {
			return Long.parseLong(subject) > 0;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
