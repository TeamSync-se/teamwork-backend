package com.teamwork.backend.common.error;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Mã lỗi chung, khớp bảng "Mã lỗi chung" ở mục 5 của {@code docs/api/conventions.md}.
 * Đổi tên hay xóa mã ở đây là đổi hợp đồng: phải sửa tài liệu trước.
 */
public enum CommonErrorCode implements ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu yêu cầu"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ cho đường dẫn này"),
	PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Tệp tải lên vượt quá dung lượng cho phép"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Kiểu dữ liệu gửi lên không được hỗ trợ"),
	EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "Dịch vụ bên ngoài đang gặp lỗi, vui lòng thử lại sau"),
	/** Hợp đồng dùng chung mã {@code EXTERNAL_SERVICE_ERROR} cho cả 502 và 503. */
	EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE,
			"Dịch vụ bên ngoài không phản hồi, vui lòng thử lại sau"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau");

	private final String code;

	private final HttpStatus status;

	private final String defaultMessage;

	CommonErrorCode(HttpStatus status, String defaultMessage) {
		this.code = name();
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	CommonErrorCode(String code, HttpStatus status, String defaultMessage) {
		this.code = code;
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public HttpStatus status() {
		return this.status;
	}

	@Override
	public String defaultMessage() {
		return this.defaultMessage;
	}

	/**
	 * Mã chung ứng với một mã HTTP do Spring sinh ra. Lỗi 5xx không có trong bảng đều coi là
	 * {@link #INTERNAL_ERROR}; lỗi 4xx không có trong bảng (ví dụ 406) trả về rỗng.
	 */
	static Optional<CommonErrorCode> forStatus(HttpStatusCode status) {
		return switch (status.value()) {
			case 400 -> Optional.of(BAD_REQUEST);
			case 401 -> Optional.of(UNAUTHORIZED);
			case 403 -> Optional.of(FORBIDDEN);
			case 404 -> Optional.of(NOT_FOUND);
			case 405 -> Optional.of(METHOD_NOT_ALLOWED);
			case 413 -> Optional.of(PAYLOAD_TOO_LARGE);
			case 415 -> Optional.of(UNSUPPORTED_MEDIA_TYPE);
			case 502 -> Optional.of(EXTERNAL_SERVICE_ERROR);
			case 503 -> Optional.of(EXTERNAL_SERVICE_UNAVAILABLE);
			default -> status.is5xxServerError() ? Optional.of(INTERNAL_ERROR) : Optional.empty();
		};
	}

}
