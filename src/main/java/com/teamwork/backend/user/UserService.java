package com.teamwork.backend.user;

import com.teamwork.backend.common.error.BusinessException;
import com.teamwork.backend.common.error.CommonErrorCode;
import com.teamwork.backend.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Thông tin người đang đăng nhập, để frontend kiểm tra phiên.
	 *
	 * @param userId id lấy từ token, không lấy từ dữ liệu frontend gửi lên
	 * @throws BusinessException với mã {@code UNAUTHORIZED} khi token còn hạn nhưng tài khoản không còn tồn tại;
	 * frontend nhận 401 thì xóa token và chuyển về trang Đăng nhập
	 */
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(Long userId) {
		return this.userRepository.findById(userId)
			.map(UserResponse::from)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
	}

}
