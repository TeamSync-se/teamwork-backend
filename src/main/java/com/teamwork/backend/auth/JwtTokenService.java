package com.teamwork.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.teamwork.backend.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Phát hành access token (JWT) cho người dùng đã đăng nhập. Claim {@code sub} là id người dùng; xem
 * {@link CurrentUserId} để lấy lại id này trong controller.
 */
@Service
public class JwtTokenService {

	/** Theo mục 2 của {@code docs/api/auth.md}: token hết hạn sau 1 ngày, không có refresh token. */
	static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(1);

	private final JwtEncoder jwtEncoder;

	public JwtTokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	/** Thời điểm tính theo giây, vì JWT lưu {@code iat}, {@code exp} theo giây. */
	public Jwt issueAccessToken(User user) {
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(user.getId().toString())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(ACCESS_TOKEN_TTL))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
	}

}
