package com.teamwork.backend.config;

import java.io.IOException;

import com.teamwork.backend.common.error.ApiProblems;
import com.teamwork.backend.common.error.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Trả lỗi 401 và 403 do Spring Security sinh ra theo định dạng lỗi chung (mục 5 của
 * {@code docs/api/conventions.md}). Các lỗi này xảy ra trong bộ lọc, trước khi tới controller, nên
 * {@code GlobalExceptionHandler} không bắt được.
 */
final class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	/** Đặt mã 401 và header {@code WWW-Authenticate: Bearer ...} theo RFC 6750. */
	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

	private final JacksonJsonHttpMessageConverter converter;

	/**
	 * Dùng {@code JsonMapper} của ứng dụng để JSON giống hệt lỗi do controller trả. Converter dựng từ builder
	 * mới có sẵn cách ghi {@link ProblemDetail} (đưa {@code code}, {@code timestamp} ra cấp ngoài cùng).
	 */
	SecurityErrorHandler(JsonMapper jsonMapper) {
		this.converter = new JacksonJsonHttpMessageConverter(jsonMapper.rebuild());
	}

	/** Thiếu token, token sai hoặc hết hạn (UC02 kịch bản 4b). */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		this.bearerEntryPoint.commence(request, response, authException);
		write(response, ApiProblems.of(CommonErrorCode.UNAUTHORIZED, request.getRequestURI()));
	}

	/** Đã đăng nhập nhưng không đủ quyền. */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(response, ApiProblems.of(CommonErrorCode.FORBIDDEN, request.getRequestURI()));
	}

	private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
		response.setStatus(problem.getStatus());
		try (ServletServerHttpResponse output = new ServletServerHttpResponse(response)) {
			this.converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, output);
		}
	}

}
