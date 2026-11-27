package com.teamwork.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình Cloudflare R2 (lưu tệp), đọc từ các khóa {@code app.storage.*} trong {@code application.properties},
 * lấy giá trị từ các biến môi trường {@code R2_*}.
 * <p>
 * Chưa có tính năng nào dùng R2 nên mọi biến đều tùy chọn. Khi làm tính năng tải tệp, chuyển các biến cần thiết
 * sang bắt buộc theo quy ước ở {@link ExternalServicesConfig}.
 *
 * @param accountId mã tài khoản Cloudflare sở hữu bucket ({@code R2_ACCOUNT_ID})
 * @param accessKeyId Access Key ID của token R2 ({@code R2_ACCESS_KEY_ID})
 * @param secretAccessKey Secret Access Key đi kèm ({@code R2_SECRET_ACCESS_KEY})
 * @param bucket tên bucket lưu tệp ({@code R2_BUCKET})
 */
@Validated
@ConfigurationProperties("app.storage")
public record StorageProperties(String accountId, String accessKeyId, String secretAccessKey, String bucket) {

	/** Không đưa khóa vào log. */
	@Override
	public String toString() {
		return "StorageProperties[accountId=" + this.accountId + ", accessKeyId=******, secretAccessKey=******, bucket="
				+ this.bucket + "]";
	}

}
