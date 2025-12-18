package growdy.mumuri.service;
import growdy.mumuri.aws.S3Upload;
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

import java.time.Duration;
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
    private final S3Upload s3Upload;

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

        Slice<ChatMessage> slice = (cursor == null)
                ? chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable)
                : chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageable);

        List<ChatMessageResponse> messages = slice.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(m -> ChatMessageResponse.from(m, resolveImageUrl(m.getImageUrl())))
                .toList();

        Long nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatMessage last = slice.getContent().get(slice.getNumberOfElements() - 1);
            nextCursor = last.getId();
        }

        return new ChatHistoryResponse(messages, slice.hasNext(), nextCursor);
    }
    private String resolveImageUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;

        // 외부 URL / 이미 presigned면 그대로
        if (stored.startsWith("http")) return stored;

        // ✅ stored가 S3 key일 때만 presigned 발급
        return s3Upload.presignedGetUrl(stored, Duration.ofMinutes(10));
    }
}