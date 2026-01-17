package growdy.mumuri;


import growdy.mumuri.login.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import growdy.mumuri.repository.*;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final CoupleMissionRepository coupleMissionRepository;
    private final CoupleMissionProgressRepository coupleMissionProgressRepository;
    private final CouplePhotoRepository couplePhotoRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MissionScheduleRepository missionScheduleRepository;
    private final PhotoRepository photoRepository;
    @PersistenceContext
    private EntityManager em;

    @Value("${app.init.truncate:false}")
    private boolean truncateEnabled;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetDatabaseExceptMissions() {
        if (!truncateEnabled) {
            return;
        }

        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        em.createNativeQuery("TRUNCATE TABLE chat_message").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE chat_room").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE couple_mission_progress").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE couple_mission").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE couple_photo").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE mission_schedule").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE photo").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE couple").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE member").executeUpdate();

        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }

}
