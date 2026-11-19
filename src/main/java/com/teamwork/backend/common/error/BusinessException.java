package com.teamwork.backend.common.error;

/**
 * Lỗi nghiệp vụ do service ném ra; {@link GlobalExceptionHandler} đổi nó thành response lỗi chung
 * với mã HTTP và {@code code} lấy từ {@link ErrorCode}.
 * <p>
 * Thông điệp được trả nguyên văn trong trường {@code detail} cho người dùng đọc, nên phải viết
 * tiếng Việt và không chứa chi tiết nội bộ (tên lớp, câu lệnh SQL, dữ liệu của người khác).
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.defaultMessage());
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

}
