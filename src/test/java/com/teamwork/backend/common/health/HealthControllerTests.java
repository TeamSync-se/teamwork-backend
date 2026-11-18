package com.teamwork.backend.common.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamwork.backend.config.CorsConfig;
import com.teamwork.backend.config.JwtConfig;
import com.teamwork.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Dùng cấu hình Security thật của ứng dụng để chứng minh {@code /api/health} không cần đăng nhập.
 */
@WebMvcTest(HealthController.class)
@Import({ SecurityConfig.class, JwtConfig.class, CorsConfig.class })
class HealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthReturnsUpWithoutLogin() throws Exception {
		this.mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
	}

}
