package growdy.mumuri.dto;

import growdy.mumuri.domain.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String message;
    private String imageUrl;
    //private String emojiUrl;
    private LocalDateTime sentAt;
    private boolean isRead;

    public static ChatMessageResponse from(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSender().getId(),
                msg.getMessage(),
                msg.getImageUrl(),
                msg.getCreatedAt(),
                msg.isRead()
        );
    }
}

