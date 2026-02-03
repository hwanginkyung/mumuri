package growdy.mumuri.service;
import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.*;
import growdy.mumuri.dto.ChatHistoryResponse;
import growdy.mumuri.dto.ChatMessageDto;
import growdy.mumuri.dto.ChatMessageResponse;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.ChatMessageRepository;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static growdy.mumuri.util.BlurKeyUtil.toBlurKey;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final S3Upload s3Upload;
    private final CoupleMissionRepository coupleMissionRepository;
    private static final String BLUR_MESSAGE = "상대방이 보낸 사진을 보려면 먼저 같은 미션을 수행해야 해요.";

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
    public ChatHistoryResponse getHistory(Long roomId, Long cursor, int size, Long viewerId) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));

        Slice<ChatMessage> slice = (cursor == null)
                ? chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, pageable)
                : chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, pageable);

        // ✅ 이번 페이지에 등장한 missionHistoryId들 한번에 조회해서 status 맵 생성
        Map<Long, MissionStatus> statusByMissionId = slice.getContent().stream()
                .map(ChatMessage::getMissionHistoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        ids -> ids.isEmpty()
                                ? Collections.emptyMap()
                                : coupleMissionRepository.findAllById(ids).stream()
                                .collect(Collectors.toMap(CoupleMission::getId, CoupleMission::getStatus))
                ));

        List<ChatMessageResponse> messages = slice.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getId))
                .map(m -> {
                    boolean shouldBlurForViewer = shouldBlurForViewer(m, viewerId, statusByMissionId);
                    return ChatMessageResponse.from(
                            m,
                            resolveImageUrl(m, shouldBlurForViewer),
                            shouldBlurForViewer,
                            shouldBlurForViewer ? BLUR_MESSAGE : null
                    );
                })
                .toList();

        Long nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            ChatMessage last = slice.getContent().get(slice.getNumberOfElements() - 1);
            nextCursor = last.getId();
        }

        return new ChatHistoryResponse(messages, slice.hasNext(), nextCursor);
    }

    private String resolveImageUrl(ChatMessage m, boolean shouldBlurForViewer) {
        String stored = m.getImageUrl();
        if (stored == null || stored.isBlank()) return null;

        if (stored.startsWith("http")) return stored;

        String keyToExpose = shouldBlurForViewer ? toBlurKey(stored) : stored;
        return s3Upload.presignedGetUrl(keyToExpose, Duration.ofMinutes(10));
    }

    private boolean shouldBlurForViewer(ChatMessage m, Long viewerId, Map<Long, MissionStatus> statusByMissionId) {
        return viewerId != null
                && m.getType() == ChatMessageType.MISSION_IMAGE
                && m.getSender() != null
                && m.getSender().getId() != null
                && !m.getSender().getId().equals(viewerId)
                && m.getMissionHistoryId() != null
                && statusByMissionId.get(m.getMissionHistoryId()) == MissionStatus.HALF_DONE;
    }
}
