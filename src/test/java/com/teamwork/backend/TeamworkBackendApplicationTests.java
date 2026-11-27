package com.teamwork.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TeamworkBackendApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
	}

	/**
	 * Spring Boot chỉ tạo tài khoản mặc định, và in "Using generated security password" khi khởi động, lúc ứng
	 * dụng chưa có cách xác thực nào. Đăng nhập bằng JWT thay cho tài khoản đó.
	 */
	@Test
	void noDefaultAccountWithGeneratedPassword() {
		assertThat(this.context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
	}

}
