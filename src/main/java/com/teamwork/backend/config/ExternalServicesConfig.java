package com.teamwork.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bật các lớp cấu hình dịch vụ ngoài để Spring đọc và kiểm tra chúng ngay khi khởi động, kể cả khi chưa có tính
 * năng nào dùng tới: cấu hình sai thì backend dừng ngay, không đợi đến lần gọi dịch vụ đầu tiên.
 * <p>
 * Quy ước cho các lớp cấu hình có chứa khóa:
 * <ul>
 * <li>Biến bắt buộc: thêm {@code @NotBlank} với {@code message} nêu tên biến môi trường, ví dụ
 * {@code @NotBlank(message = "Chưa đặt biến môi trường R2_BUCKET, xem README.md")}. Khi thiếu biến, Spring chỉ in
 * ra giá trị rỗng.</li>
 * <li>Kiểm tra nội dung khóa (độ dài, định dạng): viết trong constructor của record, <b>không</b> dùng
 * {@code @Size} hay {@code @Pattern}. Khi một ràng buộc như vậy bị vi phạm, Spring in nguyên giá trị bị từ chối
 * ra log, tức là in luôn khóa.</li>
 * <li>{@code toString()} che các trường chứa khóa.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ StorageProperties.class, AiProperties.class, MailProperties.class })
public class ExternalServicesConfig {

}
