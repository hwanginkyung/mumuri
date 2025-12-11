package growdy.mumuri;


import growdy.mumuri.login.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetDatabaseExceptMissions() {
        System.out.println("🧹 Initializing DB... Deleting all except mission table.");

        // 🚨 1️⃣ FK 체크 비활성화 (개발용)
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        // 🚨 2️⃣ 참조 관계 순서대로 삭제 (하위 → 상위)
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();

        coupleMissionProgressRepository.deleteAll();
        coupleMissionRepository.deleteAll();
        couplePhotoRepository.deleteAll();
        missionScheduleRepository.deleteAll();
        photoRepository.deleteAll();

        coupleRepository.deleteAll();
        memberRepository.deleteAll();

        // 🚨 3️⃣ FK 체크 다시 활성화
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        System.out.println("✅ DB reset complete (missions preserved).");



    }
}

