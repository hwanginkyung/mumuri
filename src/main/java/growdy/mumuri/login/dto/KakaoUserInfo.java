package growdy.mumuri.login.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record KakaoUserInfo(String id, String email, String nickname) {
    public static KakaoUserInfo from(JsonNode json) {
        JsonNode kakaoAccount = json.get("kakao_account");
        JsonNode profile = kakaoAccount.get("profile");

        return new KakaoUserInfo(
                json.get("id").asText(),
                kakaoAccount.get("email").asText(),
                profile.get("nickname").asText()
        );
    }
}