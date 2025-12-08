package growdy.mumuri.service;
import growdy.mumuri.domain.ChatMessage;
import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.ChatHistoryResponse;
import growdy.mumuri.dto.ChatMessageDto;
import growdy.mumuri.dto.ChatMessageResponse;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.ChatMessageRepository;
import growdy.mumuri.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
        ChatMessage saved = chatMessageRepository.save(message);
        log.info("[chat save] roomId={}, messageId={}", room.getId(), saved.getId());
        return saved;
    }

    public void markAsRead(Long roomId, Long userId) {
        chatMessageRepository.markMessagesAsRead(roomId, userId);
        log.info("📩 markAsRead 실행: roomId={}, userId={}", roomId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(Long roomId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));

        Slice<ChatMessage> slice;
        if (cursor == null) {
            // 첫 요청: 가장 최신 메시지부터 size개
            slice = chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable);
        } else {
            // 다음 요청: cursor(마지막으로 본 메시지 id) 보다 이전 것들
            slice = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageable);
        }

        // DB에서는 id DESC 로 가져왔지만,
        // 화면에서는 오래된 것부터 보이게 ASC로 한 번 뒤집어 줌
        List<ChatMessageResponse> messages = slice.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(ChatMessageResponse::from)  // ✅ 기존 from(ChatMessage) 그대로 사용
                .toList();

        Long nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatMessage last = slice.getContent().get(slice.getNumberOfElements() - 1);
            nextCursor = last.getId();
        }

        return new ChatHistoryResponse(messages, slice.hasNext(), nextCursor);
    }
}