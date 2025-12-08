package growdy.mumuri.repository;

import growdy.mumuri.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
            "WHERE m.chatRoom.id = :roomId AND m.sender.id <> :userId AND m.isRead = false")
    void markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long roomId);


    // ✅ 최근 메시지 50개 같은 느낌으로 가져오기 (id DESC)
    Slice<ChatMessage> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    // ✅ cursor(이전 메시지 id) 기준으로 그 이전 것들 가져오기
    Slice<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(
            Long chatRoomId,
            Long id,
            Pageable pageable
    );
}
