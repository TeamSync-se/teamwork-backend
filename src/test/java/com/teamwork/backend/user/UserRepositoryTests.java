package com.teamwork.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teamwork.backend.TestcontainersConfiguration;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void deleteAllUsers() {
		this.userRepository.deleteAll();
	}

	/**
	 * {@code AuthService.register} dựa vào tên ràng buộc này để trả 409 thay vì 500 khi hai request đăng ký cùng
	 * email đến gần như cùng lúc.
	 */
	@Test
	void savingSameEmailTwiceViolatesEmailUniqueConstraint() {
		this.userRepository.saveAndFlush(new User("Nguyễn Văn A", "a@example.com", "$2a$10$hash"));

		assertThatThrownBy(
				() -> this.userRepository.saveAndFlush(new User("Trần Thị B", "a@example.com", "$2a$10$hash")))
			.isInstanceOf(DataIntegrityViolationException.class)
			.cause()
			.isInstanceOfSatisfying(ConstraintViolationException.class,
					(violation) -> assertThat(violation.getConstraintName()).isEqualTo(User.EMAIL_UNIQUE_CONSTRAINT));
	}

}
