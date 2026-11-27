package com.teamwork.backend.user;

import com.teamwork.backend.auth.CurrentUserId;
import com.teamwork.backend.user.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API về người dùng, theo mục 3 của {@code docs/api/auth.md}. Cần đăng nhập.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public UserResponse me(@CurrentUserId Long userId) {
		return this.userService.getCurrentUser(userId);
	}

}
