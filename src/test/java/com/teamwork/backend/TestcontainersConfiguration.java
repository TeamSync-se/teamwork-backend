package com.teamwork.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Cấu hình test dùng chung: mỗi lần chạy test, Testcontainers bật một PostgreSQL tạm trong Docker
 * và Spring Boot tự kết nối tới nó thay cho cơ sở dữ liệu ở localhost:5432.
 * Test cần cơ sở dữ liệu chỉ cần thêm {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// Cùng bản với compose.yaml
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

}
