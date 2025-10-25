package growdy.mumuri.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {
    private Long roomId;
    private Long senderId;
    private String message;
    private String imageUrl;
    //private Long emojiId; // optional
}
