package growdy.mumuri.login.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AppleUserInfo(
        String id,
        String email,
        String name
) {
    public static AppleUserInfo from(JsonNode node) {
        String id = node.path("sub").asText(null);
        String email = node.path("email").asText(null);
        String name = node.path("name").asText(null);
        return new AppleUserInfo(id, email, name);
    }
}
