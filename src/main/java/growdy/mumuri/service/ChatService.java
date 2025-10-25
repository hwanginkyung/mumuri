package growdy.mumuri.service;
import growdy.mumuri.domain.ChatMessage;
import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.ChatMessageDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.ChatMessageRepository;
import growdy.mumuri.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    public ChatMessage saveMessage(ChatMessageDto dto) {
        Member sender = memberRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));
        ChatRoom room = chatRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방"));

        ChatMessage message = new ChatMessage(room, sender, dto.getMessage());
        return chatMessageRepository.save(message);
    }

    public void markAsRead(Long roomId, Long userId) {
        chatMessageRepository.markMessagesAsRead(roomId, userId);
        log.info("📩 markAsRead 실행: roomId={}, userId={}", roomId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
    }
}