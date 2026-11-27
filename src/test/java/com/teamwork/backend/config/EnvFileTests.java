package com.teamwork.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Khi dev, backend tự đọc file {@code .env} ở thư mục gốc; khi triển khai, biến môi trường của hệ thống được
 * dùng và được ưu tiên hơn {@code .env}.
 * <p>
 * Dòng import trong {@code application.properties} trỏ tới {@code .env} ở thư mục đang chạy. Test không được
 * ghi vào {@code .env} thật trên máy, nên các test hành vi dùng đúng dòng import đó nhưng trỏ sang một thư mục
 * tạm, và thay biến môi trường của máy bằng một danh sách do test đặt.
 */
class EnvFileTests {

	@TempDir
	Path tempDir;

	@Test
	void applicationPropertiesImportsEnvFileFromWorkingDirectory() {
		assertThat(envFileImport()).isEqualTo("optional:file:.env[.properties]");
	}

	@Test
	void variablesInEnvFileAreUsed() {
		writeEnvFile("""
				# Dòng chú thích bị bỏ qua
				JWT_SECRET=abc+/def==
				R2_BUCKET = teamwork-dev
				""");

		ConfigurableEnvironment environment = start(Map.of());

		assertThat(environment.resolvePlaceholders("${JWT_SECRET:}")).isEqualTo("abc+/def==");
		assertThat(environment.resolvePlaceholders("${R2_BUCKET:}")).isEqualTo("teamwork-dev");
	}

	@Test
	void systemEnvironmentVariableOverridesEnvFile() {
		writeEnvFile("R2_BUCKET=bucket-trong-file-env\n");

		ConfigurableEnvironment environment = start(Map.of("R2_BUCKET", "bucket-cua-he-thong"));

		assertThat(environment.resolvePlaceholders("${R2_BUCKET:}")).isEqualTo("bucket-cua-he-thong");
	}

	@Test
	void missingEnvFileDoesNotStopStartup() {
		ConfigurableEnvironment environment = start(Map.of("R2_BUCKET", "bucket-cua-he-thong"));

		assertThat(environment.resolvePlaceholders("${R2_BUCKET:}")).isEqualTo("bucket-cua-he-thong");
		assertThat(environment.resolvePlaceholders("${JWT_SECRET:}")).isEmpty();
	}

	/**
	 * Khởi động Spring Boot với dòng import {@code .env} thật, trỏ sang thư mục tạm. Không nạp
	 * {@code application.properties} để test không đọc {@code .env} thật ở thư mục gốc.
	 */
	private ConfigurableEnvironment start(Map<String, String> systemEnvironment) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.replace(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, new SystemEnvironmentPropertySource(
					SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, new HashMap<>(systemEnvironment)));
		SpringApplication application = new SpringApplication(EmptyConfiguration.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setBannerMode(Banner.Mode.OFF);
		application.setEnvironment(environment);

		String importInTempDir = envFileImport().replace("file:.env",
				"file:" + this.tempDir.toAbsolutePath().toString().replace('\\', '/') + "/.env");
		try (ConfigurableApplicationContext context = application.run(
				"--spring.config.name=khong-nap-application-properties", "--spring.config.import=" + importInTempDir)) {
			return context.getEnvironment();
		}
	}

	private void writeEnvFile(String content) {
		try {
			Files.writeString(this.tempDir.resolve(".env"), content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String envFileImport() {
		try {
			return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))
				.getProperty("spring.config.import");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

}
