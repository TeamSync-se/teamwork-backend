package com.teamwork.backend.config;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamwork.backend.common.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CORS với origin mặc định trong {@code application.properties} ({@code http://localhost:3000}). Header
 * {@code Origin} giả lập request trình duyệt gửi từ trang frontend; trình duyệt chỉ cho trang đọc response
 * khi có header {@code Access-Control-Allow-Origin} khớp với origin của trang.
 */
@WebMvcTest(HealthController.class)
@Import({ SecurityConfig.class, JwtConfig.class, CorsConfig.class })
class CorsConfigTests {

	private static final String FRONTEND_ORIGIN = "http://localhost:3000";

	private static final String OTHER_ORIGIN = "http://evil.example.com";

	/** Đường dẫn chưa có API; Spring Security từ chối trước khi tìm tới controller nên không cần controller thật. */
	private static final String PROTECTED_API = "/api/groups";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void frontendOriginCanReadHealthResponse() throws Exception {
		this.mockMvc.perform(get("/api/health").header(ORIGIN, FRONTEND_ORIGIN))
			.andExpect(status().isOk())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void preflightFromFrontendOriginListsAllowedMethods() throws Exception {
		this.mockMvc
			.perform(options("/api/health").header(ORIGIN, FRONTEND_ORIGIN)
				.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	void requestFromOtherOriginIsRejectedWithoutCorsHeader() throws Exception {
		this.mockMvc.perform(get("/api/health").header(ORIGIN, OTHER_ORIGIN))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void preflightFromOtherOriginIsRejectedWithoutCorsHeader() throws Exception {
		this.mockMvc
			.perform(options("/api/health").header(ORIGIN, OTHER_ORIGIN).header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void preflightToProtectedApiSucceedsWithoutToken() throws Exception {
		this.mockMvc
			.perform(options(PROTECTED_API).header(ORIGIN, FRONTEND_ORIGIN)
				.header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("Authorization")))
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("Content-Type")));
	}

	@Test
	void protectedApiWithoutTokenReturns401WithCorsHeader() throws Exception {
		this.mockMvc.perform(get(PROTECTED_API).header(ORIGIN, FRONTEND_ORIGIN))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));
	}

}
