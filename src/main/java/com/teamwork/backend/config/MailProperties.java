package com.teamwork.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình máy chủ SMTP gửi email, đọc từ các khóa {@code app.mail.*} trong {@code application.properties}, lấy giá
 * trị từ các biến môi trường {@code MAIL_*}.
 * <p>
 * Chưa có tính năng nào gửi email nên mọi biến đều tùy chọn; riêng cổng, nếu có đặt, phải là số cổng hợp lệ. Khi
 * làm tính năng gửi email, chuyển các biến cần thiết sang bắt buộc theo quy ước ở {@link ExternalServicesConfig}.
 *
 * @param host máy chủ SMTP ({@code MAIL_HOST})
 * @param port cổng SMTP ({@code MAIL_PORT}); {@code null} khi chưa đặt
 * @param username tài khoản đăng nhập SMTP ({@code MAIL_USERNAME})
 * @param password mật khẩu SMTP ({@code MAIL_PASSWORD})
 */
@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(String host,
		@Min(value = 1, message = MailProperties.INVALID_PORT) @Max(value = 65535,
				message = MailProperties.INVALID_PORT) Integer port,
		String username, String password) {

	static final String INVALID_PORT = "MAIL_PORT phải là số cổng từ 1 đến 65535";

	/** Không đưa mật khẩu vào log. */
	@Override
	public String toString() {
		return "MailProperties[host=" + this.host + ", port=" + this.port + ", username=" + this.username
				+ ", password=******]";
	}

}
