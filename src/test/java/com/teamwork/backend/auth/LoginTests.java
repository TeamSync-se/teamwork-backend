package com.teamwork.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import com.teamwork.backend.TestcontainersConfiguration;
import com.teamwork.backend.user.User;
import com.teamwork.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Đăng nhập ({@code POST /api/auth/login}, UC02) với cơ sở dữ liệu thật và cấu hình Spring Security thật của
 * ứng dụng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginTests {

	private static final String LOGIN_PATH = "/api/auth/login";

	private static final String FULL_NAME = "Nguyễn Văn A";

	private static final String EMAIL = "a@example.com";

	private static final String PASSWORD = "matkhau123";

	private static final String INVALID_CREDENTIALS_MESSAGE = "Email hoặc mật khẩu không chính xác";

	private static final String ISO_UTC_SECONDS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	private User user;

	@BeforeEach
	void createAccount() {
		this.userRepository.deleteAll();
		this.user = this.userRepository.save(new User(FULL_NAME, EMAIL, this.passwordEncoder.encode(PASSWORD)));
	}

	@Test
	void validCredentialsReturn200WithTokenValidForOneDay() throws Exception {
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String body = this.mockMvc.perform(login(json(EMAIL, PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			// Đúng 3 trường của hợp đồng, không có trường nào khác
			.andExpect(jsonPath("$.*", hasSize(3)))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.expiresAt", matchesPattern(ISO_UTC_SECONDS)))
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		Instant after = Instant.now();

		Instant expiresAt = Instant.parse(JsonPath.read(body, "$.expiresAt"));
		assertThat(expiresAt).isBetween(before.plus(Duration.ofDays(1)), after.plus(Duration.ofDays(1)));
		Jwt token = this.jwtDecoder.decode(JsonPath.read(body, "$.accessToken"));
		assertThat(token.getSubject()).isEqualTo(this.user.getId().toString());
		assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
	}

	/** Luồng của frontend (docs/api/auth.md mục 4): đăng nhập, rồi dùng token gọi {@code GET /api/users/me}. */
	@Test
	void tokenFromLoginCanCallProtectedApi() throws Exception {
		String body = this.mockMvc.perform(login(json(EMAIL, PASSWORD)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		String accessToken = JsonPath.read(body, "$.accessToken");

		this.mockMvc.perform(get("/api/users/me").header(AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(this.user.getId()))
			.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void emailIsCaseInsensitiveAndTrimmed() throws Exception {
		this.mockMvc.perform(login(json("  A@Example.COM ", PASSWORD))).andExpect(status().isOk());
	}

	/** Sai email và sai mật khẩu trả response giống hệt nhau (UC02 kịch bản 4a). */
	@ParameterizedTest(name = "{0}")
	@MethodSource("wrongCredentials")
	void wrongEmailOrPasswordReturnsSame401(String description, String email, String password) throws Exception {
		this.mockMvc.perform(login(json(email, password)))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value(INVALID_CREDENTIALS_MESSAGE))
			.andExpect(jsonPath("$.instance").value(LOGIN_PATH))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	static Stream<Arguments> wrongCredentials() {
		return Stream.of(arguments("sai mật khẩu", EMAIL, "matkhau-sai"),
				arguments("mật khẩu phân biệt chữ hoa, chữ thường", EMAIL, PASSWORD.toUpperCase()),
				arguments("email chưa đăng ký", "chua-dang-ky@example.com", PASSWORD));
	}

	/**
	 * BCrypt chỉ so 72 byte đầu, nên "mật khẩu đúng + ký tự thừa" khớp với mã băm. Backend phải tự từ chối mật
	 * khẩu dài hơn 72 byte.
	 */
	@Test
	void passwordLongerThan72BytesIsRejectedEvenWhenFirst72BytesMatch() throws Exception {
		String password72Bytes = "ệ".repeat(24);
		String passwordHash = this.passwordEncoder.encode(password72Bytes);
		this.userRepository.save(new User("Trần Thị B", "b@example.com", passwordHash));
		assertThat(this.passwordEncoder.matches(password72Bytes + "x", passwordHash)).isTrue();

		this.mockMvc.perform(login(json("b@example.com", password72Bytes))).andExpect(status().isOk());
		this.mockMvc.perform(login(json("b@example.com", password72Bytes + "x")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value(INVALID_CREDENTIALS_MESSAGE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRequests")
	void invalidRequestReturns400WithFieldError(String body, String field, String message) throws Exception {
		this.mockMvc.perform(login(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value(field))
			.andExpect(jsonPath("$.errors[0].message").value(message));
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(arguments(json(null, PASSWORD), "email", "Email không được để trống"),
				arguments(json("   ", PASSWORD), "email", "Email không được để trống"),
				arguments(json("khong-phai-email", PASSWORD), "email", "Email không đúng định dạng"),
				arguments(json(EMAIL, null), "password", "Mật khẩu không được để trống"),
				arguments(json(EMAIL, ""), "password", "Mật khẩu không được để trống"));
	}

	/** Đăng ký cho phép mật khẩu gồm toàn khoảng trắng, nên đăng nhập không được coi đó là để trống. */
	@Test
	void passwordOfOnlySpacesIsAccepted() throws Exception {
		this.userRepository.save(new User("Trần Thị B", "b@example.com", this.passwordEncoder.encode("        ")));

		this.mockMvc.perform(login(json("b@example.com", "        "))).andExpect(status().isOk());
	}

	@Test
	void wrongHttpMethodReturns405() throws Exception {
		this.mockMvc.perform(get(LOGIN_PATH))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	private static MockHttpServletRequestBuilder login(String body) {
		return post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static String json(String email, String password) {
		return "{\"email\":" + quote(email) + ",\"password\":" + quote(password) + "}";
	}

	private static String quote(String value) {
		return (value != null) ? "\"" + value + "\"" : "null";
	}

}
