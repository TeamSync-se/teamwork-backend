package com.teamwork.backend.auth;

import com.teamwork.backend.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi riêng của phân hệ tài khoản, khớp bảng lỗi trong {@code docs/api/auth.md}. Đổi tên hay xóa mã ở đây
 * là đổi hợp đồng: phải sửa tài liệu trước.
 */
public enum AuthErrorCode implements ErrorCode {

	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng");

	private final HttpStatus status;

	private final String defaultMessage;

	AuthErrorCode(HttpStatus status, String defaultMessage) {
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
