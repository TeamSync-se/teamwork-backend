package com.teamwork.backend.user;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import com.teamwork.backend.TestcontainersConfiguration;
import com.teamwork.backend.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Thông tin người đang đăng nhập ({@code GET /api/users/me}) với cơ sở dữ liệu thật và cấu hình Spring Security
 * thật của ứng dụng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerTests {

	private static final String ME_PATH = "/api/users/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	@Autowired
	private JwtEncoder jwtEncoder;

	private User userA;

	private User userB;

	@BeforeEach
	void createAccounts() {
		this.userRepository.deleteAll();
		this.userA = this.userRepository.save(new User("Nguyễn Văn A", "a@example.com", "$2a$10$hashA"));
		this.userB = this.userRepository.save(new User("Trần Thị B", "b@example.com", "$2a$10$hashB"));
	}

	/** Người gọi được xác định từ token, nên mỗi token chỉ thấy đúng tài khoản của mình. */
	@Test
	void meReturnsAccountOfTokenOwner() throws Exception {
		this.mockMvc.perform(get(ME_PATH).header(AUTHORIZATION, bearerTokenOf(this.userB)))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			// Đúng 4 trường của hợp đồng; không có mật khẩu hay mã băm
			.andExpect(jsonPath("$.*", hasSize(4)))
			.andExpect(jsonPath("$.id").value(this.userB.getId()))
			.andExpect(jsonPath("$.fullName").value("Trần Thị B"))
			.andExpect(jsonPath("$.email").value("b@example.com"))
			.andExpect(jsonPath("$.createdAt").value(this.userB.getCreatedAt().toString()));
	}

	@Test
	void meWithoutTokenReturns401() throws Exception {
		this.mockMvc.perform(get(ME_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void meWithExpiredTokenReturns401() throws Exception {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(this.userA.getId().toString())
			.issuedAt(now.minus(Duration.ofDays(2)))
			.expiresAt(now.minus(Duration.ofDays(1)))
			.build();
		String expiredToken = this.jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		this.mockMvc.perform(get(ME_PATH).header(AUTHORIZATION, "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	/** Token còn hạn nhưng tài khoản không còn: frontend nhận 401 và đưa người dùng về trang Đăng nhập. */
	@Test
	void tokenOfDeletedAccountReturns401() throws Exception {
		String token = bearerTokenOf(this.userA);
		this.userRepository.delete(this.userA);

		this.mockMvc.perform(get(ME_PATH).header(AUTHORIZATION, token))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.instance").value(ME_PATH));
	}

	private String bearerTokenOf(User user) {
		return "Bearer " + this.jwtTokenService.issueAccessToken(user).getTokenValue();
	}

}
