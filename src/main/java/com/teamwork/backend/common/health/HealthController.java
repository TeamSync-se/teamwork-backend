package com.teamwork.backend.common.health;

import com.teamwork.backend.common.health.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cho frontend kiểm tra backend còn chạy. Không cần đăng nhập và không kiểm tra cơ sở dữ liệu: chỉ xác nhận
 * ứng dụng đang nhận request.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public HealthResponse health() {
		return new HealthResponse("UP");
	}

}
