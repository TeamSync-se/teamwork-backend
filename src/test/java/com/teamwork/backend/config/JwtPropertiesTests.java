package com.teamwork.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Khóa ký token lấy từ biến môi trường {@code JWT_SECRET}; thiếu hoặc quá ngắn thì backend không khởi động.
 */
@ExtendWith(OutputCaptureExtension.class)
class JwtPropertiesTests {

	private static final String SECRET = "khoa-dung-trong-JwtPropertiesTests-dai-hon-32-byte";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(JwtConfig.class);

	/**
	 * Nạp {@code application.properties} thật của ứng dụng (không có file cấu hình riêng của test), rồi đặt
	 * {@code JWT_SECRET} như biến môi trường.
	 */
	@Test
	void secretIsReadFromJwtSecretEnvironmentVariable() {
		this.contextRunner.withInitializer((context) -> context.getEnvironment()
			.getPropertySources()
			.addLast(applicationProperties()))
			.withPropertyValues("JWT_SECRET=" + SECRET)
			.run((context) -> assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(SECRET));
	}

	/** Đặt rỗng thay vì bỏ hẳn, để test không phụ thuộc máy chạy test có sẵn biến JWT_SECRET hay không. */
	@Test
	void missingSecretStopsStartup() {
		this.contextRunner.withInitializer((context) -> context.getEnvironment()
			.getPropertySources()
			.addLast(applicationProperties()))
			.withPropertyValues("JWT_SECRET=")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("Chưa đặt biến môi trường JWT_SECRET"));
	}

	@Test
	void secretShorterThan32BytesStopsStartup() {
		this.contextRunner.withPropertyValues("app.jwt.secret=" + "a".repeat(31))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("ít nhất 32 byte"));
	}

	/**
	 * Khởi động {@link SpringApplication} thật, vì thông báo "APPLICATION FAILED TO START" chỉ được in khi chạy qua
	 * {@link SpringApplication}. Chỉ nạp {@code application.properties} của ứng dụng; tham số dòng lệnh được ưu
	 * tiên hơn biến {@code JWT_SECRET} có sẵn trên máy chạy test.
	 */
	@Test
	void failedStartupDoesNotWriteSecretToLog(CapturedOutput output) {
		String shortSecret = "khoa-ngan-khong-duoc-in-ra-log"; // 30 byte

		SpringApplication application = new SpringApplication(JwtConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		assertThatException().isThrownBy(() -> application
			.run("--spring.config.location=classpath:/application.properties", "--JWT_SECRET=" + shortSecret)
			.close());

		assertThat(output).contains("APPLICATION FAILED TO START")
			.contains("JWT_SECRET phải dài ít nhất 32 byte")
			.doesNotContain(shortSecret);
	}

	@Test
	void tokenSignedWithSecretCanBeVerified() {
		this.contextRunner.withPropertyValues("app.jwt.secret=" + SECRET).run((context) -> {
			JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("1")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
			String token = context.getBean(JwtEncoder.class)
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();

			assertThat(context.getBean(JwtDecoder.class).decode(token).getSubject()).isEqualTo("1");
		});
	}

	@Test
	void toStringDoesNotRevealSecret() {
		assertThat(new JwtProperties(SECRET).toString()).doesNotContain(SECRET);
	}

	private static ResourcePropertySource applicationProperties() {
		try {
			return new ResourcePropertySource(new ClassPathResource("application.properties"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
