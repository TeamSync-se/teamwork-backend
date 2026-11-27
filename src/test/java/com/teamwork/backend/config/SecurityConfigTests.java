package com.teamwork.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import com.teamwork.backend.auth.CurrentUserId;
import com.teamwork.backend.common.error.CommonErrorCode;
import com.teamwork.backend.common.health.HealthController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cấu hình Spring Security thật của ứng dụng, với controller chỉ có trong test để không cần cơ sở dữ liệu. Token
 * được ký bằng chính {@link JwtEncoder} của ứng dụng, trừ khi test cần token sai.
 */
@WebMvcTest(controllers = { SecurityConfigTests.TestController.class, HealthController.class })
@Import({ SecurityConfig.class, JwtConfig.class, CorsConfig.class, SecurityConfigTests.TestController.class })
class SecurityConfigTests {

	private static final String PROTECTED_PATH = "/api/test/protected";

	private static final long USER_ID = 42;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void validTokenReachesControllerWithUserIdFromToken() throws Exception {
		this.mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, bearer(validToken())))
			.andExpect(status().isOk())
			.andExpect(content().string(String.valueOf(USER_ID)));
	}

	/** Kể cả đường dẫn chưa có API: trả 401 chứ không phải 404, để không lộ API nào tồn tại. */
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("protectedEndpoints")
	void requestWithoutTokenReturns401InCommonFormat(HttpMethod method, String path) throws Exception {
		expectUnauthorized(this.mockMvc.perform(request(method, path)), path)
			.andExpect(header().string(WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	static Stream<Arguments> protectedEndpoints() {
		return Stream.of(arguments(HttpMethod.GET, PROTECTED_PATH), arguments(HttpMethod.GET, "/api/groups"),
				arguments(HttpMethod.POST, "/api/groups"),
				// Không có API đăng xuất (docs/api/auth.md mục 4)
				arguments(HttpMethod.POST, "/logout"));
	}

	@Test
	void malformedTokenReturns401() throws Exception {
		expectRejected("khong-phai-jwt");
	}

	@Test
	void tokenSignedWithAnotherKeyReturns401() throws Exception {
		JwtEncoder otherEncoder = NimbusJwtEncoder
			.withSecretKey(new SecretKeySpec("mot-khoa-khac-hoan-toan-dai-hon-32-byte".getBytes(StandardCharsets.UTF_8),
					"HmacSHA256"))
			.algorithm(MacAlgorithm.HS256)
			.build();
		Instant now = Instant.now();

		expectRejected(encode(otherEncoder, now, now.plus(Duration.ofHours(1))));
	}

	/** Token không có chữ ký ({@code "alg": "none"}) mà vẫn được chấp nhận thì ai cũng tự tạo được token. */
	@Test
	void unsignedTokenReturns401() throws Exception {
		expectRejected("eyJhbGciOiJub25lIn0.eyJzdWIiOiI0MiJ9.");
	}

	@Test
	void expiredTokenReturns401() throws Exception {
		Instant now = Instant.now();

		expectRejected(encode(this.jwtEncoder, now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1))));
	}

	/**
	 * Token hết hạn đúng thời điểm {@code expiresAt}: mặc định Spring còn nhận token quá hạn dưới 60 giây, nên test
	 * này chứng minh {@link JwtConfig} đã bỏ khoảng đó.
	 */
	@Test
	void tokenExpiredAFewSecondsAgoReturns401() throws Exception {
		Instant now = Instant.now();

		expectRejected(encode(this.jwtEncoder, now.minus(Duration.ofDays(1)), now.minusSeconds(5)));
	}

	/** Token không có {@code exp} thì không bao giờ hết hạn, nên phải bị từ chối dù chữ ký đúng. */
	@Test
	void tokenWithoutExpirationReturns401() throws Exception {
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(String.valueOf(USER_ID)).issuedAt(Instant.now()).build();

		expectRejected(encode(this.jwtEncoder, claims));
	}

	/**
	 * {@code sub} phải là id người dùng (số nguyên dương). Sai thì trả 401 ngay khi kiểm tra token, không để lỗi
	 * 500 khi {@link CurrentUserId} đổi {@code sub} sang số.
	 */
	@ParameterizedTest(name = "sub = \"{0}\"")
	@NullSource
	@ValueSource(strings = { "invalid", "", "0", "-1", "1.5", "99999999999999999999" })
	void tokenWithInvalidSubjectReturns401(String subject) throws Exception {
		Instant now = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuedAt(now).expiresAt(now.plus(Duration.ofHours(1)));
		if (subject != null) {
			claims.subject(subject);
		}

		expectRejected(encode(this.jwtEncoder, claims.build()));
	}

	@Test
	void accessDeniedReturns403InCommonFormat() throws Exception {
		this.mockMvc.perform(get("/api/test/forbidden").header(AUTHORIZATION, bearer(validToken())))
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("about:blank"))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.detail").value(CommonErrorCode.FORBIDDEN.defaultMessage()))
			.andExpect(jsonPath("$.instance").value("/api/test/forbidden"))
			.andExpect(jsonPath("$.timestamp").isString())
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	/** Token cũ hoặc hỏng còn trong cookie của frontend không được chặn API công khai. */
	@Test
	void publicEndpointIgnoresInvalidToken() throws Exception {
		this.mockMvc.perform(get("/api/health").header(AUTHORIZATION, bearer("khong-phai-jwt")))
			.andExpect(status().isOk());
	}

	@Test
	void noSessionIsCreated() throws Exception {
		MvcResult result = this.mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, bearer(validToken())))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist(SET_COOKIE))
			.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
	}

	private void expectRejected(String token) throws Exception {
		expectUnauthorized(this.mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, bearer(token))),
				PROTECTED_PATH)
			.andExpect(header().string(WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")));
	}

	private static ResultActions expectUnauthorized(ResultActions result, String path) throws Exception {
		return result.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("about:blank"))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.detail").value(CommonErrorCode.UNAUTHORIZED.defaultMessage()))
			.andExpect(jsonPath("$.instance").value(path))
			.andExpect(jsonPath("$.timestamp").isString())
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	private String validToken() {
		Instant now = Instant.now();
		return encode(this.jwtEncoder, now, now.plus(Duration.ofHours(1)));
	}

	private static String encode(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
		return encode(encoder, JwtClaimsSet.builder()
			.subject(String.valueOf(USER_ID))
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build());
	}

	private static String encode(JwtEncoder encoder, JwtClaimsSet claims) {
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	@RestController
	static class TestController {

		@GetMapping(PROTECTED_PATH)
		Long protectedEndpoint(@CurrentUserId Long userId) {
			return userId;
		}

		@GetMapping("/api/test/forbidden")
		void forbidden() {
			throw new AccessDeniedException("Không đủ quyền");
		}

	}

}
