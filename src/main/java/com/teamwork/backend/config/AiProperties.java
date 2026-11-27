package com.teamwork.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình dịch vụ AI tóm tắt nội dung, đọc từ các khóa {@code app.ai.*} trong {@code application.properties},
 * lấy giá trị từ các biến môi trường {@code AI_*}.
 * <p>
 * Chưa có tính năng nào dùng AI nên mọi biến đều tùy chọn. Khi làm tính năng tóm tắt, chuyển các biến cần thiết
 * sang bắt buộc theo quy ước ở {@link ExternalServicesConfig}.
 *
 * @param apiKey API key của dịch vụ AI ({@code AI_API_KEY})
 * @param model tên model dùng để tóm tắt ({@code AI_MODEL})
 */
@Validated
@ConfigurationProperties("app.ai")
public record AiProperties(String apiKey, String model) {

	/** Không đưa khóa vào log. */
	@Override
	public String toString() {
		return "AiProperties[apiKey=******, model=" + this.model + "]";
	}

}
