package com.teamwork.backend.common.error;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Đổi mọi lỗi phát sinh trong controller thành response lỗi chung (mục 5 của
 * {@code docs/api/conventions.md}).
 * <p>
 * Kế thừa {@link ResponseEntityExceptionHandler} để các lỗi chuẩn của Spring MVC (JSON hỏng, sai
 * phương thức, đường dẫn không tồn tại...) cũng có {@code code} và thông điệp tiếng Việt.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {
		ProblemDetail problem = ApiProblems.of(ex.getErrorCode(), ex.getMessage(), pathOf(request));
		return respond(ex, problem, new HttpHeaders(), request);
	}

	/** Lỗi kiểm tra dữ liệu ở tầng service ({@code @Validated}). */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			errors.add(new FieldErrorDetail(lastNodeName(violation.getPropertyPath()), violation.getMessage()));
		}
		return respond(ex, ApiProblems.validation(errors, pathOf(request)), new HttpHeaders(), request);
	}

	/**
	 * Lỗi xác thực và phân quyền để Spring Security tự xử lý (trả 401 hoặc 403). Nếu không ném lại,
	 * {@link #handleUnexpected} sẽ bắt và biến chúng thành lỗi 500.
	 */
	@ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
	public void rethrowSecurityException(RuntimeException ex) {
		throw ex;
	}

	/** Lỗi không lường trước: chỉ trả thông điệp chung, chi tiết ghi vào log. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		ProblemDetail problem = ApiProblems.of(CommonErrorCode.INTERNAL_ERROR, pathOf(request));
		return respond(ex, problem, new HttpHeaders(), request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return respond(ex, ApiProblems.validation(toFieldErrors(ex.getBindingResult()), pathOf(request)), headers,
				request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			if (result instanceof ParameterErrors parameterErrors) {
				errors.addAll(toFieldErrors(parameterErrors));
			}
			else {
				String parameterName = result.getMethodParameter().getParameterName();
				result.getResolvableErrors()
					.forEach(error -> errors.add(new FieldErrorDetail(parameterName, error.getDefaultMessage())));
			}
		}
		ex.getCrossParameterValidationResults()
			.forEach(error -> errors.add(new FieldErrorDetail(null, error.getDefaultMessage())));
		return respond(ex, ApiProblems.validation(errors, pathOf(request)), headers, request);
	}

	/** Mọi lỗi chuẩn khác của Spring MVC đi qua đây; giữ mã HTTP và header (ví dụ {@code Allow}). */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return respond(ex, ApiProblems.forStatus(statusCode, pathOf(request)), headers, request);
	}

	private ResponseEntity<Object> respond(Exception ex, ProblemDetail problem, HttpHeaders headers,
			WebRequest request) {
		HttpStatusCode status = HttpStatusCode.valueOf(problem.getStatus());
		if (status.is5xxServerError()) {
			log.error("Lỗi {} khi xử lý {}", status.value(), pathOf(request), ex);
		}
		return super.handleExceptionInternal(ex, problem, headers, status, request);
	}

	private static List<FieldErrorDetail> toFieldErrors(Errors validationErrors) {
		List<FieldErrorDetail> errors = new ArrayList<>();
		validationErrors.getFieldErrors()
			.forEach(error -> errors.add(new FieldErrorDetail(error.getField(), error.getDefaultMessage())));
		validationErrors.getGlobalErrors()
			.forEach(error -> errors.add(new FieldErrorDetail(null, error.getDefaultMessage())));
		return errors;
	}

	private static String lastNodeName(Path path) {
		String name = null;
		for (Path.Node node : path) {
			name = node.getName();
		}
		return name;
	}

	private static String pathOf(WebRequest request) {
		return (request instanceof ServletWebRequest servletRequest) ? servletRequest.getRequest().getRequestURI()
				: null;
	}

}
