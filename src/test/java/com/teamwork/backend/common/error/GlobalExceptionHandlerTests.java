package com.teamwork.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Kiểm tra định dạng lỗi chung (mục 5 của docs/api/conventions.md) bằng một controller chỉ dùng
 * trong test, ném ra từng loại lỗi. Không cần cơ sở dữ liệu.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTests.ErrorTestController.class)
@Import(GlobalExceptionHandlerTests.ErrorTestController.class)
@WithMockUser
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTests {

	private static final String ISO_UTC_SECONDS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

	private static final String SECRET = "db-secret-host:5432";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void invalidRequestBodyReturns400WithErrorPerField() throws Exception {
		this.mockMvc
			.perform(post("/test-errors/sign-up").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fullName": "   ", "email": "khong-phai-email"}
						"""))
			.andExpectAll(problem(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", "/test-errors/sign-up"))
			.andExpect(jsonPath("$.errors.length()").value(2))
			.andExpect(jsonPath("$.errors[?(@.field == 'fullName')].message", hasItem("Họ tên không được để trống")))
			.andExpect(jsonPath("$.errors[?(@.field == 'email')].message", hasItem("Email không đúng định dạng")));
	}

	@Test
	void invalidRequestParameterReturns400WithParameterName() throws Exception {
		this.mockMvc.perform(get("/test-errors/items").param("size", "0"))
			.andExpectAll(problem(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", "/test-errors/items"))
			.andExpect(jsonPath("$.errors[0].field").value("size"))
			.andExpect(jsonPath("$.errors[0].message").value("Kích thước trang phải từ 1 trở lên"));
	}

	@Test
	void constraintViolationFromServiceReturns400WithErrorPerField() throws Exception {
		this.mockMvc.perform(get("/test-errors/service-validation"))
			.andExpectAll(problem(400, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", "/test-errors/service-validation"))
			.andExpect(jsonPath("$.errors[?(@.field == 'fullName')].message", hasItem("Họ tên không được để trống")))
			.andExpect(jsonPath("$.errors[?(@.field == 'email')].message", hasItem("Email không đúng định dạng")));
	}

	@Test
	void malformedJsonReturns400BadRequestWithoutErrors() throws Exception {
		this.mockMvc
			.perform(post("/test-errors/sign-up").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\": "))
			.andExpectAll(problem(400, "BAD_REQUEST", CommonErrorCode.BAD_REQUEST.defaultMessage(),
					"/test-errors/sign-up"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void unknownPathReturns404() throws Exception {
		this.mockMvc.perform(get("/api/khong-ton-tai"))
			.andExpectAll(problem(404, "NOT_FOUND", CommonErrorCode.NOT_FOUND.defaultMessage(), "/api/khong-ton-tai"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void businessNotFoundReturns404() throws Exception {
		this.mockMvc.perform(get("/test-errors/business/not-found"))
			.andExpectAll(problem(404, "NOT_FOUND", "Không tìm thấy nhóm", "/test-errors/business/not-found"));
	}

	@Test
	void businessForbiddenReturns403() throws Exception {
		this.mockMvc.perform(get("/test-errors/business/forbidden"))
			.andExpectAll(problem(403, "FORBIDDEN", CommonErrorCode.FORBIDDEN.defaultMessage(),
					"/test-errors/business/forbidden"));
	}

	@Test
	void moduleErrorCodeIsReturnedWithItsOwnCodeAndStatus() throws Exception {
		this.mockMvc.perform(get("/test-errors/business/email-taken"))
			.andExpectAll(problem(409, "EMAIL_ALREADY_EXISTS", "Email đã được sử dụng",
					"/test-errors/business/email-taken"))
			.andExpect(jsonPath("$.title").value("Conflict"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void wrongHttpMethodReturns405AndKeepsAllowHeader() throws Exception {
		this.mockMvc.perform(delete("/test-errors/sign-up").with(csrf()))
			.andExpectAll(problem(405, "METHOD_NOT_ALLOWED", CommonErrorCode.METHOD_NOT_ALLOWED.defaultMessage(),
					"/test-errors/sign-up"))
			.andExpect(header().string("Allow", containsString("POST")));
	}

	@Test
	void bodyWithoutJsonContentTypeReturns415() throws Exception {
		this.mockMvc
			.perform(post("/test-errors/sign-up").with(csrf()).contentType(MediaType.TEXT_PLAIN).content("abc"))
			.andExpectAll(problem(415, "UNSUPPORTED_MEDIA_TYPE",
					CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage(), "/test-errors/sign-up"));
	}

	@Test
	void oversizedUploadReturns413() throws Exception {
		this.mockMvc.perform(get("/test-errors/too-large"))
			.andExpectAll(problem(413, "PAYLOAD_TOO_LARGE", CommonErrorCode.PAYLOAD_TOO_LARGE.defaultMessage(),
					"/test-errors/too-large"));
	}

	@ParameterizedTest
	@CsvSource({ "502, Dịch vụ lưu tệp đang gặp lỗi", "503, Dịch vụ AI không phản hồi" })
	void externalServiceFailureReturns502Or503WithSameCode(int status, String message) throws Exception {
		this.mockMvc.perform(get("/test-errors/external/{status}", status))
			.andExpectAll(problem(status, "EXTERNAL_SERVICE_ERROR", message, "/test-errors/external/" + status));
	}

	@Test
	void unexpectedErrorReturns500WithoutInternalDetailsAndLogsThem(CapturedOutput output) throws Exception {
		String body = this.mockMvc.perform(get("/test-errors/unexpected"))
			.andExpectAll(problem(500, "INTERNAL_ERROR", CommonErrorCode.INTERNAL_ERROR.defaultMessage(),
					"/test-errors/unexpected"))
			.andExpect(jsonPath("$.trace").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain(SECRET, "IllegalStateException", "ErrorTestController");
		assertThat(output).contains(SECRET, "IllegalStateException");
	}

	@Test
	void accessDeniedIsLeftToSpringSecurityInsteadOfBecoming500() throws Exception {
		this.mockMvc.perform(get("/test-errors/access-denied")).andExpect(status().isForbidden());
	}

	private static ResultMatcher[] problem(int status, String code, String detail, String instance) {
		return new ResultMatcher[] { status().is(status),
				content().contentType(MediaType.APPLICATION_PROBLEM_JSON),
				jsonPath("$.type").value("about:blank"),
				jsonPath("$.title").value(HttpStatus.valueOf(status).getReasonPhrase()),
				jsonPath("$.status").value(status),
				jsonPath("$.detail").value(detail),
				jsonPath("$.instance").value(instance),
				jsonPath("$.code").value(code),
				jsonPath("$.timestamp").value(matchesPattern(ISO_UTC_SECONDS)) };
	}

	record SignUpRequest(@NotBlank(message = "Họ tên không được để trống") String fullName,
			@NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email) {
	}

	/** Mã lỗi riêng của một phân hệ, giống cách phân hệ tài khoản khai báo {@code EMAIL_ALREADY_EXISTS}. */
	enum TestErrorCode implements ErrorCode {

		EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng");

		private final HttpStatus status;

		private final String defaultMessage;

		TestErrorCode(HttpStatus status, String defaultMessage) {
			this.status = status;
			this.defaultMessage = defaultMessage;
		}

		@Override
		public String code() {
			return name();
		}

		@Override
		public HttpStatus status() {
			return this.status;
		}

		@Override
		public String defaultMessage() {
			return this.defaultMessage;
		}

	}

	@RestController
	@RequestMapping("/test-errors")
	static class ErrorTestController {

		@PostMapping("/sign-up")
		void signUp(@Valid @RequestBody SignUpRequest request) {
		}

		@GetMapping("/items")
		void items(@RequestParam @Min(value = 1, message = "Kích thước trang phải từ 1 trở lên") int size) {
		}

		@GetMapping("/service-validation")
		void serviceValidation() {
			try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
				throw new ConstraintViolationException(
						factory.getValidator().validate(new SignUpRequest(" ", "khong-phai-email")));
			}
		}

		@GetMapping("/business/not-found")
		void businessNotFound() {
			throw new BusinessException(CommonErrorCode.NOT_FOUND, "Không tìm thấy nhóm");
		}

		@GetMapping("/business/forbidden")
		void businessForbidden() {
			throw new BusinessException(CommonErrorCode.FORBIDDEN);
		}

		@GetMapping("/business/email-taken")
		void businessEmailTaken() {
			throw new BusinessException(TestErrorCode.EMAIL_ALREADY_EXISTS);
		}

		@GetMapping("/external/{status}")
		void external(@PathVariable int status) {
			if (status == 502) {
				throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, "Dịch vụ lưu tệp đang gặp lỗi");
			}
			throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "Dịch vụ AI không phản hồi");
		}

		@GetMapping("/too-large")
		void tooLarge() {
			throw new MaxUploadSizeExceededException(1024);
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("Không kết nối được tới " + SECRET);
		}

		@GetMapping("/access-denied")
		void accessDenied() {
			throw new AccessDeniedException("Không có quyền");
		}

	}

}
