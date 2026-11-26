package com.teamwork.backend.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thay trang lỗi mặc định ({@code /error}) của Spring Boot, để lỗi phát sinh ngoài controller (trong
 * filter, hoặc khi gọi {@code response.sendError}) cũng trả cùng định dạng lỗi chung.
 * <p>
 * Không bao giờ đưa thông điệp của exception vào response; exception đã được server ghi log.
 */
@RestController
public class ProblemDetailErrorController implements ErrorController {

	@RequestMapping("${server.error.path:${error.path:/error}}")
	public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
		Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		ProblemDetail problem = ApiProblems.forStatus(statusOf(request),
				(path != null) ? path.toString() : request.getRequestURI());
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	private static HttpStatusCode statusOf(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		return (status instanceof Integer code) ? HttpStatusCode.valueOf(code) : HttpStatus.INTERNAL_SERVER_ERROR;
	}

}
