package growdy.mumuri.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Builder
@AllArgsConstructor
public class ChatMessage extends BaseEntity{
    @Id
    @GeneratedValue
    private long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member sender;

    private String message;
    private String imageUrl;

    public ChatMessage(ChatRoom room, Member sender, String message) {
        this.chatRoom = room;
        this.sender = sender;
        this.message = message;
    }
    private boolean isRead = false;
}
