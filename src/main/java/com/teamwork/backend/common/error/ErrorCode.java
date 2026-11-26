package com.teamwork.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Một mã lỗi trả cho frontend, theo mục 5 của {@code docs/api/conventions.md}.
 * Mã chung nằm ở {@link CommonErrorCode}; mỗi phân hệ tự khai báo enum riêng cho mã của mình
 * (ví dụ {@code EMAIL_ALREADY_EXISTS}) và ghi mã đó vào file hợp đồng của phân hệ.
 */
public interface ErrorCode {

	/** Mã dạng chữ, cố định, frontend dựa vào đây để xử lý. */
	String code();

	HttpStatus status();

	/** Thông điệp tiếng Việt hiển thị cho người dùng khi không truyền thông điệp riêng. */
	String defaultMessage();

}
