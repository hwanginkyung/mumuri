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
public class ChatMessage extends BaseEntity {
    @Id @GeneratedValue
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member sender;

    @Enumerated(EnumType.STRING)
    private ChatMessageType type = ChatMessageType.CHAT;   // ✅ 기본은 일반 채팅

    private String message;
    private String imageUrl;

    private Long missionHistoryId;  // ✅ 미션 완료 이벤트면 연결 (선택)

    private boolean isRead = false;

    public ChatMessage(ChatRoom room, Member sender, String message) {
        this.chatRoom = room;
        this.sender = sender;
        this.message = message;
        this.type = ChatMessageType.CHAT;
    }

    /*public static ChatMessage missionDone(ChatRoom room, Member performer, Long historyId, String title, String imageUrl) {
        ChatMessage m = new ChatMessage(room, performer, title);
        m.type = ChatMessageType.MISSION_DONE;
        m.missionHistoryId = historyId;
        m.imageUrl = imageUrl;
        return m;
    }*/
}

