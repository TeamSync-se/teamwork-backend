package com.teamwork.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ALLOW;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;
import com.teamwork.backend.TestcontainersConfiguration;
import com.teamwork.backend.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Đăng ký tài khoản ({@code POST /api/auth/register}, UC01) với cơ sở dữ liệu thật và cấu hình Spring Security
 * thật của ứng dụng. Không request nào gửi kèm token hay token CSRF, để chứng minh khách gọi được API này.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

	/**
	 * Múi giờ khác UTC cho cả lớp, để {@link #createdAtIsStoredInUtc()} bắt được lỗi lưu giờ địa phương kể cả
	 * trên máy CI (thường đặt giờ UTC). Phải đặt trước khi Spring tạo context và mở kết nối CSDL.
	 */
	private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

	private static TimeZone originalTimeZone;

	private static final String REGISTER_PATH = "/api/auth/register";

	private static final String FULL_NAME = "Nguyễn Văn A";

	private static final String EMAIL = "a@example.com";

	private static final String PASSWORD = "matkhau123";

	private static final String ISO_UTC_SECONDS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeAll
	static void useNonUtcTimeZone() {
		originalTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TEST_TIME_ZONE);
	}

	@AfterAll
	static void restoreTimeZone() {
		TimeZone.setDefault(originalTimeZone);
	}

	@BeforeEach
	void deleteAllUsers() {
		this.userRepository.deleteAll();
	}

	@Test
	void registerWithValidDataReturns201WithAccountInfo() throws Exception {
		this.mockMvc.perform(register(json("  " + FULL_NAME + "  ", "  A@Example.COM ", PASSWORD, PASSWORD)))
			.andExpect(status().isCreated())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			// Đúng 4 trường của hợp đồng, không có trường nào khác
			.andExpect(jsonPath("$.*", hasSize(4)))
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.fullName").value(FULL_NAME))
			.andExpect(jsonPath("$.email").value(EMAIL))
			.andExpect(jsonPath("$.createdAt", matchesPattern(ISO_UTC_SECONDS)));
		assertThat(this.userRepository.count()).isEqualTo(1);
	}

	@Test
	void passwordIsStoredAsBcryptHashAndNeverReturned() throws Exception {
		String body = this.mockMvc.perform(register(json(FULL_NAME, EMAIL, PASSWORD, PASSWORD)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		String passwordHash = this.jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?",
				String.class, EMAIL);
		assertThat(passwordHash).isNotEqualTo(PASSWORD).startsWith("$2");
		assertThat(this.passwordEncoder.matches(PASSWORD, passwordHash)).isTrue();
		assertThat(body).doesNotContain(PASSWORD).doesNotContain(passwordHash).doesNotContainIgnoringCase("password");
	}

	/** Cột {@code created_at} không kèm múi giờ: giá trị lưu phải là giờ UTC, trùng với {@code createdAt} trả về. */
	@Test
	void createdAtIsStoredInUtc() throws Exception {
		String body = this.mockMvc.perform(register(json(FULL_NAME, EMAIL, PASSWORD, PASSWORD)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		String storedCreatedAt = this.jdbcTemplate.queryForObject(
				"SELECT to_char(created_at, 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') FROM users WHERE email = ?", String.class,
				EMAIL);
		assertThat(storedCreatedAt).isEqualTo(JsonPath.read(body, "$.createdAt"));
	}

	@Test
	void guestCanRegisterFromFrontendOriginWithoutToken() throws Exception {
		this.mockMvc
			.perform(register(json(FULL_NAME, EMAIL, PASSWORD, PASSWORD)).header(ORIGIN, "http://localhost:3000"))
			.andExpect(status().isCreated())
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	@Test
	void existingEmailWithDifferentCaseReturns409() throws Exception {
		this.mockMvc.perform(register(json(FULL_NAME, EMAIL, PASSWORD, PASSWORD))).andExpect(status().isCreated());

		this.mockMvc.perform(register(json("Trần Thị B", "A@EXAMPLE.com", "matkhau456", "matkhau456")))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
			.andExpect(jsonPath("$.detail").value("Email đã được sử dụng"))
			.andExpect(jsonPath("$.instance").value(REGISTER_PATH))
			.andExpect(jsonPath("$.timestamp", matchesPattern(ISO_UTC_SECONDS)))
			.andExpect(jsonPath("$.errors").doesNotExist());
		assertThat(this.userRepository.count()).isEqualTo(1);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("requestsWithOneInvalidField")
	void invalidFieldReturns400WithErrorOnThatField(String scenario, String body, String field, String message)
			throws Exception {
		this.mockMvc.perform(register(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.instance").value(REGISTER_PATH))
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value(field))
			.andExpect(jsonPath("$.errors[0].message").value(message));
		assertThat(this.userRepository.count()).isZero();
	}

	static Stream<Arguments> requestsWithOneInvalidField() {
		String tooManyBytes = "ệ".repeat(25); // 25 chữ "ệ": đủ số ký tự nhưng chiếm 75 byte
		return Stream.of(
				arguments("họ tên trống", json("", EMAIL, PASSWORD, PASSWORD), "fullName",
						"Họ tên không được để trống"),
				arguments("họ tên chỉ có khoảng trắng", json("   ", EMAIL, PASSWORD, PASSWORD), "fullName",
						"Họ tên không được để trống"),
				arguments("thiếu họ tên", json(null, EMAIL, PASSWORD, PASSWORD), "fullName",
						"Họ tên không được để trống"),
				arguments("họ tên quá 100 ký tự", json("a".repeat(101), EMAIL, PASSWORD, PASSWORD), "fullName",
						"Họ tên tối đa 100 ký tự"),
				arguments("email sai định dạng", json(FULL_NAME, "khong-phai-email", PASSWORD, PASSWORD), "email",
						"Email không đúng định dạng"),
				arguments("thiếu email", json(FULL_NAME, null, PASSWORD, PASSWORD), "email",
						"Email không được để trống"),
				arguments("mật khẩu dưới 8 ký tự", json(FULL_NAME, EMAIL, "1234567", "1234567"), "password",
						"Mật khẩu phải có ít nhất 8 ký tự"),
				arguments("mật khẩu quá 64 ký tự", json(FULL_NAME, EMAIL, "a".repeat(65), "a".repeat(65)), "password",
						"Mật khẩu tối đa 64 ký tự"),
				// Vượt cả 64 ký tự lẫn 72 byte: chỉ báo một lỗi
				arguments("mật khẩu không dấu quá 72 ký tự", json(FULL_NAME, EMAIL, "a".repeat(80), "a".repeat(80)),
						"password", "Mật khẩu tối đa 64 ký tự"),
				arguments("mật khẩu có dấu quá 64 ký tự",
						json(FULL_NAME, EMAIL, "ệ".repeat(65), "ệ".repeat(65)), "password",
						"Mật khẩu tối đa 64 ký tự"),
				arguments("mật khẩu có dấu quá 72 byte", json(FULL_NAME, EMAIL, tooManyBytes, tooManyBytes), "password",
						"Mật khẩu quá dài, vui lòng rút ngắn (ký tự có dấu được tính nhiều hơn một ký tự)"),
				arguments("thiếu mật khẩu", json(FULL_NAME, EMAIL, null, PASSWORD), "password",
						"Mật khẩu không được để trống"),
				arguments("xác nhận mật khẩu không khớp", json(FULL_NAME, EMAIL, PASSWORD, "matkhau124"),
						"confirmPassword", "Mật khẩu xác nhận không khớp"),
				arguments("thiếu xác nhận mật khẩu", json(FULL_NAME, EMAIL, PASSWORD, null), "confirmPassword",
						"Vui lòng nhập lại mật khẩu"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("passwordsAtLengthLimits")
	void passwordAtLengthLimitIsAccepted(String scenario, String password) throws Exception {
		this.mockMvc.perform(register(json(FULL_NAME, EMAIL, password, password))).andExpect(status().isCreated());
	}

	static Stream<Arguments> passwordsAtLengthLimits() {
		return Stream.of(arguments("đúng 8 ký tự", "a".repeat(8)), arguments("đúng 64 ký tự", "a".repeat(64)),
				arguments("có dấu, đúng 72 byte", "ệ".repeat(24)));
	}

	@Test
	void wrongHttpMethodReturns405() throws Exception {
		this.mockMvc.perform(get(REGISTER_PATH))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
			.andExpect(header().string(ALLOW, containsString("POST")));
	}

	@Test
	void severalInvalidFieldsReturnAllErrorsAndSaveNothing() throws Exception {
		this.mockMvc.perform(register(json("  ", "khong-phai-email", "123", "456")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors[*].field",
					containsInAnyOrder("fullName", "email", "password", "confirmPassword")));
		assertThat(this.userRepository.count()).isZero();
	}

	private static MockHttpServletRequestBuilder register(String body) {
		return post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	/** Body JSON của request đăng ký; giá trị {@code null} được gửi là {@code null} trong JSON. */
	private static String json(String fullName, String email, String password, String confirmPassword) {
		return """
				{"fullName": %s, "email": %s, "password": %s, "confirmPassword": %s}
				""".formatted(quote(fullName), quote(email), quote(password), quote(confirmPassword));
	}

	private static String quote(String value) {
		return (value != null) ? "\"" + value + "\"" : "null";
	}

}
