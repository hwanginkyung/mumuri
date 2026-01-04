package growdy.mumuri.repository;

import growdy.mumuri.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    void deleteByChatRoomId(Long chatRoomId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ChatMessage m
           SET m.isRead = true
         WHERE m.chatRoom.id = :roomId
           AND m.sender.id <> :userId
           AND m.isRead = false
    """)
    void markMessagesAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // ✅ (getMessages 용) sender까지 같이 가져와서 N+1 방지
    @EntityGraph(attributePaths = {"sender"})
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long roomId);

    // ✅ (getHistory 첫 페이지) sender 같이 로딩
    @EntityGraph(attributePaths = {"sender"})
    Slice<ChatMessage> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

    // ✅ (getHistory 다음 페이지) sender 같이 로딩
    @EntityGraph(attributePaths = {"sender"})
    Slice<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(
            Long chatRoomId,
            Long id,
            Pageable pageable
    );

}
