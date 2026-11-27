package com.teamwork.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Khóa dịch vụ ngoài (R2, AI, email) chỉ được tham chiếu bằng tên biến môi trường trong
 * {@code application.properties}, được gom vào các lớp {@code @ConfigurationProperties} và được kiểm tra khi khởi
 * động.
 * <p>
 * Các test nạp {@code application.properties} thật của ứng dụng, rồi đặt biến như biến môi trường. Biến nào test
 * không cần thì đặt rỗng, để kết quả không phụ thuộc biến môi trường có sẵn trên máy chạy test.
 */
class ExternalServicePropertiesTests {

	/** Giá trị tham chiếu biến môi trường, ví dụ {@code ${R2_BUCKET:}} hoặc {@code ${CORS_ALLOWED_ORIGINS:...}}. */
	private static final Pattern VARIABLE_REFERENCE = Pattern.compile("\\$\\{[A-Z][A-Z0-9_]*(:[^}]*)?}");

	/** Tham chiếu biến môi trường không kèm giá trị mặc định, ví dụ {@code ${R2_SECRET_ACCESS_KEY:}}. */
	private static final Pattern VARIABLE_REFERENCE_WITHOUT_DEFAULT = Pattern.compile("\\$\\{[A-Z][A-Z0-9_]*:?}");

	private static final Pattern SENSITIVE_KEY = Pattern.compile(".*(secret|password|key).*");

	/**
	 * Mật khẩu mặc định {@code teamwork} là mật khẩu của PostgreSQL chạy bằng Docker trên máy dev, đã có công khai
	 * trong {@code compose.yaml}; khi triển khai thì đặt biến {@code DB_PASSWORD}.
	 */
	private static final Set<String> KEYS_WITH_PUBLIC_DEV_DEFAULT = Set.of("spring.datasource.password");

	private static final String[] NO_EXTERNAL_SERVICE_VARIABLES = { "R2_ACCOUNT_ID=", "R2_ACCESS_KEY_ID=",
			"R2_SECRET_ACCESS_KEY=", "R2_BUCKET=", "AI_API_KEY=", "AI_MODEL=", "MAIL_HOST=", "MAIL_PORT=",
			"MAIL_USERNAME=", "MAIL_PASSWORD=" };

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ExternalServicesConfig.class)
		.withInitializer((context) -> context.getEnvironment()
			.getPropertySources()
			.addLast(applicationPropertiesSource()))
		.withPropertyValues(NO_EXTERNAL_SERVICE_VARIABLES);

	@Test
	void appPropertiesOnlyReferenceEnvironmentVariables() {
		Properties properties = applicationProperties();

		assertThat(properties.stringPropertyNames()).filteredOn((key) -> key.startsWith("app."))
			.isNotEmpty()
			.allSatisfy((key) -> assertThat(properties.getProperty(key)).as(key).matches(VARIABLE_REFERENCE));
	}

	@Test
	void sensitivePropertiesHaveNoDefaultValue() {
		Properties properties = applicationProperties();

		assertThat(properties.stringPropertyNames())
			.filteredOn((key) -> SENSITIVE_KEY.matcher(key).matches() && !KEYS_WITH_PUBLIC_DEV_DEFAULT.contains(key))
			.contains("app.storage.secret-access-key", "app.ai.api-key", "app.mail.password")
			.allSatisfy((key) -> assertThat(properties.getProperty(key)).as(key)
				.matches(VARIABLE_REFERENCE_WITHOUT_DEFAULT));
	}

	@Test
	void environmentVariablesAreBoundToProperties() {
		this.contextRunner
			.withPropertyValues("R2_ACCOUNT_ID=tai-khoan", "R2_ACCESS_KEY_ID=ma-khoa", "R2_SECRET_ACCESS_KEY=khoa-bi-mat",
					"R2_BUCKET=teamwork-dev", "AI_API_KEY=khoa-ai", "AI_MODEL=model-tom-tat",
					"MAIL_HOST=smtp.example.com", "MAIL_PORT=587", "MAIL_USERNAME=nhom@example.com",
					"MAIL_PASSWORD=mat-khau-smtp")
			.run((context) -> {
				assertThat(context.getBean(StorageProperties.class))
					.isEqualTo(new StorageProperties("tai-khoan", "ma-khoa", "khoa-bi-mat", "teamwork-dev"));
				assertThat(context.getBean(AiProperties.class))
					.isEqualTo(new AiProperties("khoa-ai", "model-tom-tat"));
				assertThat(context.getBean(MailProperties.class))
					.isEqualTo(new MailProperties("smtp.example.com", 587, "nhom@example.com", "mat-khau-smtp"));
			});
	}

	/** Tính năng dùng R2, AI, email chưa làm, nên thiếu các biến này backend vẫn khởi động. */
	@Test
	void externalServiceVariablesAreOptionalForNow() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(MailProperties.class).port()).isNull();
		});
	}

	@Test
	void invalidMailPortStopsStartup() {
		this.contextRunner.withPropertyValues("MAIL_PORT=70000")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining(MailProperties.INVALID_PORT));
	}

	@Test
	void toStringDoesNotRevealSecrets() {
		assertThat(new StorageProperties("tai-khoan", "ma-khoa-r2", "khoa-bi-mat-r2", "teamwork-dev").toString())
			.contains("tai-khoan", "teamwork-dev")
			.doesNotContain("ma-khoa-r2", "khoa-bi-mat-r2");
		assertThat(new AiProperties("khoa-ai-bi-mat", "model-tom-tat").toString()).contains("model-tom-tat")
			.doesNotContain("khoa-ai-bi-mat");
		assertThat(new MailProperties("smtp.example.com", 587, "nhom@example.com", "mat-khau-smtp").toString())
			.contains("smtp.example.com", "nhom@example.com")
			.doesNotContain("mat-khau-smtp");
	}

	private static Properties applicationProperties() {
		try {
			return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static ResourcePropertySource applicationPropertiesSource() {
		try {
			return new ResourcePropertySource(new ClassPathResource("application.properties"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
