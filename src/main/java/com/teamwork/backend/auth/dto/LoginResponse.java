package com.teamwork.backend.auth.dto;

import java.time.Instant;

/**
 * Token trả về khi đăng nhập thành công, theo mục 2 của {@code docs/api/auth.md}. Frontend gửi
 * {@code accessToken} trong header {@code Authorization: Bearer <accessToken>} cho tới thời điểm
 * {@code expiresAt}.
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {

	public static LoginResponse bearer(String accessToken, Instant expiresAt) {
		return new LoginResponse(accessToken, "Bearer", expiresAt);
	}

}
