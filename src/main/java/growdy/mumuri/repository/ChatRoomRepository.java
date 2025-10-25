package growdy.mumuri.repository;

import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByCouple(Couple couple);
}
