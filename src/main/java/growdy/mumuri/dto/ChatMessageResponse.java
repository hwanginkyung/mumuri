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
        boolean isRead,
        LocalDateTime createdAt,
        Long missionHistoryId
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return from(m, m.getImageUrl());
    }
    public static ChatMessageResponse from(ChatMessage m, String resolvedImageUrl) {
        return new ChatMessageResponse(
                m.getId(),
                m.getType().name(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getMessage(),
                m.getImageUrl(),
                m.isRead(),
                m.getCreatedAt(),
                m.getMissionHistoryId()
        );
    }
}
