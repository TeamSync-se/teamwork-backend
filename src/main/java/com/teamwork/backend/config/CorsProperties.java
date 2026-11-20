package com.teamwork.backend.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình CORS, đọc từ các khóa {@code app.cors.*}.
 * <p>
 * Giá trị mặc định nằm trong {@code application.properties}, không viết trong code. Thiếu khóa thì danh
 * sách rỗng, nghĩa là trình duyệt ở origin khác bị chặn hết.
 *
 * @param allowedOrigins các origin được phép gọi API từ trình duyệt, ví dụ {@code http://localhost:3000}
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = (allowedOrigins != null) ? List.copyOf(allowedOrigins) : List.of();
	}

}
