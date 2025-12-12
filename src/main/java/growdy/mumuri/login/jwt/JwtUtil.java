package growdy.mumuri.login.jwt;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    // 실무에서 보통 이렇게 많이 씀
    private static final long ACCESS_EXPIRATION  = 1000L * 60 * 30;        // 30분
    private static final long REFRESH_EXPIRATION = 1000L * 60 * 60 * 24 * 14; // 14일

    public String createAccessToken(Long userId) {
        return createToken(userId, "access", ACCESS_EXPIRATION);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, "refresh", REFRESH_EXPIRATION);
    }

    private String createToken(Long userId, String type, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .addClaims(Map.of("type", type))
                .signWith(SignatureAlgorithm.HS512, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }
    public String getId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractType(String token) {
        Object type = parseToken(token).get("type");
        return type != null ? type.toString() : null;
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractType(token));
    }
}
