package com.teamwork.backend.common.error;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Nơi duy nhất dựng body lỗi theo mục 5 của {@code docs/api/conventions.md}:
 * {@code ProblemDetail} (RFC 9457) có thêm {@code code}, {@code timestamp} và, chỉ với
 * {@code VALIDATION_ERROR}, {@code errors}.
 * <p>
 * Ngoài {@link GlobalExceptionHandler}, các thành phần nằm ngoài controller (ví dụ
 * {@code AuthenticationEntryPoint} của Spring Security) cũng dùng lớp này để trả cùng định dạng.
 */
public final class ApiProblems {

	public static final String CODE = "code";

	public static final String TIMESTAMP = "timestamp";

	public static final String ERRORS = "errors";

	/** Spring 7 để {@code type} trống và bỏ khỏi JSON; hợp đồng ghi rõ {@code "type": "about:blank"}. */
	private static final URI ABOUT_BLANK = URI.create("about:blank");

	private ApiProblems() {
	}

	public static ProblemDetail of(ErrorCode errorCode, String path) {
		return of(errorCode, errorCode.defaultMessage(), path);
	}

	public static ProblemDetail of(ErrorCode errorCode, String detail, String path) {
		return build(errorCode.status(), errorCode.code(), detail, path);
	}

	public static ProblemDetail validation(List<FieldErrorDetail> errors, String path) {
		ProblemDetail problem = of(CommonErrorCode.VALIDATION_ERROR, path);
		problem.setProperty(ERRORS, errors);
		return problem;
	}

	/**
	 * Body lỗi cho một mã HTTP do Spring sinh ra. Thông điệp luôn lấy từ mã lỗi chung (tiếng Việt),
	 * không dùng thông điệp tiếng Anh của Spring vì có thể chứa chi tiết nội bộ.
	 */
	public static ProblemDetail forStatus(HttpStatusCode status, String path) {
		return CommonErrorCode.forStatus(status)
			.map(errorCode -> of(errorCode, path))
			.orElseGet(() -> build(status, codeName(status), CommonErrorCode.BAD_REQUEST.defaultMessage(), path));
	}

	private static ProblemDetail build(HttpStatusCode status, String code, String detail, String path) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(ABOUT_BLANK);
		problem.setInstance(toUri(path));
		problem.setProperty(CODE, code);
		problem.setProperty(TIMESTAMP, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
		return problem;
	}

	private static String codeName(HttpStatusCode status) {
		HttpStatus known = HttpStatus.resolve(status.value());
		return (known != null) ? known.name() : "HTTP_" + status.value();
	}

	private static URI toUri(String path) {
		if (path == null) {
			return null;
		}
		try {
			return URI.create(path);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
