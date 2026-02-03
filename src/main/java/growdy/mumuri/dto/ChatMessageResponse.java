package growdy.mumuri.dto;

import growdy.mumuri.domain.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
public record ChatMessageResponse(
        long id,
        String type,
        Long senderId,
        String senderName,
        String message,
        String imageUrl,
        boolean blurred,
        String blurMessage,
        boolean isRead,
        LocalDateTime createdAt,
        Long missionHistoryId
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return from(m, m.getImageUrl(), false, null);
    }
    public static ChatMessageResponse from(ChatMessage m, String resolvedImageUrl, boolean blurred, String blurMessage) {
        return new ChatMessageResponse(
                m.getId(),
                m.getType().name(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getMessage(),
                resolvedImageUrl,
                blurred,
                blurMessage,
                m.isRead(),
                m.getCreatedAt(),
                m.getMissionHistoryId()
        );
    }
}
