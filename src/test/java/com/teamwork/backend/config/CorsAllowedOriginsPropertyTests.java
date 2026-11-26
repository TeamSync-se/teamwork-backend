package com.teamwork.backend.config;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamwork.backend.common.health.HealthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Danh sách origin được phép lấy từ cấu hình {@code app.cors.allowed-origins}, không viết trong code: đổi cấu
 * hình thì origin mới được phép và origin mặc định không còn được phép.
 */
@WebMvcTest(HealthController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@TestPropertySource(properties = "app.cors.allowed-origins=https://teamwork.example.com, http://127.0.0.1:3000")
class CorsAllowedOriginsPropertyTests {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = { "https://teamwork.example.com", "http://127.0.0.1:3000" })
	void everyConfiguredOriginIsAllowed(String origin) throws Exception {
		this.mockMvc.perform(get("/api/health").header(ORIGIN, origin))
			.andExpect(status().isOk())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, origin));
	}

	@Test
	void defaultOriginIsRejectedWhenNotConfigured() throws Exception {
		this.mockMvc.perform(get("/api/health").header(ORIGIN, "http://localhost:3000"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
