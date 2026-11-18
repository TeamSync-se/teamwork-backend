package com.teamwork.backend.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình token đăng nhập (JWT). Khóa ký lấy từ biến môi trường {@code JWT_SECRET} qua
 * {@code app.jwt.secret} trong {@code application.properties}.
 * <p>
 * Thiếu khóa hoặc khóa quá ngắn thì backend dừng ngay khi khởi động, thay vì chạy được rồi lỗi ở lần đăng nhập
 * đầu tiên.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret) {

	/** Thuật toán HS256 cần khóa ít nhất 256 bit (RFC 7518 mục 3.2). */
	static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException(
					"Chưa đặt biến môi trường JWT_SECRET (khóa ký token đăng nhập), xem README.md");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"JWT_SECRET phải dài ít nhất " + MIN_SECRET_BYTES + " byte, xem README.md");
		}
	}

	SecretKey secretKey() {
		return new SecretKeySpec(this.secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/** Không đưa khóa vào log, kể cả khi Spring in cấu hình lúc khởi động lỗi. */
	@Override
	public String toString() {
		return "JwtProperties[secret=******]";
	}

}
