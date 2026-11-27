package com.teamwork.backend.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Gắn vào tham số {@code Long} của controller để nhận id người đang đăng nhập, lấy từ claim {@code sub} của token
 * mà {@link JwtTokenService} phát hành. Không lấy id người gọi từ dữ liệu frontend gửi lên.
 *
 * <pre>
 * &#64;GetMapping("/me")
 * public UserResponse me(&#64;CurrentUserId Long userId) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "T(java.lang.Long).valueOf(subject)")
public @interface CurrentUserId {

}
