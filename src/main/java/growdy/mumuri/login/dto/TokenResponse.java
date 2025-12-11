package growdy.mumuri.login.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
