package com.teamwork.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Cấu hình sai thì backend dừng ngay khi khởi động, và thông báo lỗi Spring Boot in ra log không chứa giá trị khóa.
 * <p>
 * Test khởi động {@link SpringApplication} thật, vì thông báo "APPLICATION FAILED TO START" chỉ được in khi chạy
 * qua {@link SpringApplication}. Chỉ nạp {@code application.properties} của ứng dụng; biến đặt bằng tham số dòng
 * lệnh được ưu tiên hơn cả biến môi trường lẫn file {@code .env} của máy chạy test.
 */
@ExtendWith(OutputCaptureExtension.class)
class StartupFailureLogTests {

	private static final String MAIL_PASSWORD = "mat-khau-smtp-khong-duoc-xuat-hien-trong-log";

	@Test
	void failedStartupDoesNotWriteSecretsToLog(CapturedOutput output) {
		assertThatException().isThrownBy(() -> start("--MAIL_PORT=70000", "--MAIL_PASSWORD=" + MAIL_PASSWORD));

		assertThat(output).contains("APPLICATION FAILED TO START")
			.contains(MailProperties.INVALID_PORT)
			.doesNotContain(MAIL_PASSWORD);
	}

	private static void start(String... args) {
		SpringApplication application = new SpringApplication(ExternalServicesConfig.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String[] allArgs = Stream
			.concat(Stream.of("--spring.config.location=classpath:/application.properties"), Stream.of(args))
			.toArray(String[]::new);
		application.run(allArgs).close();
	}

}
