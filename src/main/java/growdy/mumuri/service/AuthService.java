package growdy.mumuri.service;

import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.RefreshToken;
import growdy.mumuri.login.dto.TokenResponse;
import growdy.mumuri.login.jwt.JwtUtil;
import growdy.mumuri.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public TokenResponse issueTokens(Long memberId, String userAgent, String ip) {
        String accessToken = jwtUtil.createAccessToken(memberId);
        String refreshToken = jwtUtil.createRefreshToken(memberId);

        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(memberId)
                .token(refreshToken)
                .expiryAt(Instant.now().plusSeconds(60L * 60 * 24 * 14))
                .userAgent(userAgent)
                .ip(ip)
                .build());

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰이 존재하지 않습니다."));

        if (stored.getExpiryAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다.");
        }

        Long memberId = jwtUtil.extractUserId(refreshToken);

        String newAccessToken = jwtUtil.createAccessToken(memberId);
        String newRefreshToken = jwtUtil.createRefreshToken(memberId);

        refreshTokenRepository.delete(stored);

        refreshTokenRepository.save(RefreshToken.builder()
                .memberId(memberId)
                .token(newRefreshToken)
                .expiryAt(Instant.now().plusSeconds(60L * 60 * 24 * 14))
                .userAgent(stored.getUserAgent())
                .ip(stored.getIp())
                .build());

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(Long memberId, String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .filter(rt -> rt.getMemberId().equals(memberId))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeAllForMember(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiryAtBefore(Instant.now());
        log.debug("Purged expired refresh tokens.");
    }
}
