package com.teamwork.backend.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Tài khoản người dùng, ánh xạ bảng {@code users} (migration {@code V1__create_users_table.sql}).
 * <p>
 * Email luôn được lưu ở dạng chữ thường; việc chuẩn hóa do service làm trước khi tạo entity.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	/**
	 * Cột {@code created_at} có kiểu {@code TIMESTAMP} (không kèm múi giờ). Hibernate gửi {@code Instant} theo
	 * giờ UTC, nên giá trị lưu là giờ UTC dù máy chạy backend ở múi giờ nào ({@code AuthControllerTests}
	 * kiểm tra điều này).
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected User() {
	}

	public User(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	/** Bỏ phần lẻ của giây, để thời điểm trả về ngay sau khi tạo giống hệt thời điểm đọc lại từ CSDL. */
	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	}

	public Long getId() {
		return this.id;
	}

	public String getFullName() {
		return this.fullName;
	}

	public String getEmail() {
		return this.email;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
