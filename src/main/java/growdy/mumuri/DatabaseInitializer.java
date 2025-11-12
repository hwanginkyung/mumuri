package growdy.mumuri;


import growdy.mumuri.login.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
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
    @PostConstruct
    @Transactional
    public void resetDatabaseExceptMissions() {
        System.out.println("🧹 Initializing DB... Deleting all except mission table.");
        em.createNativeQuery("SET FOREIGN_KEEY_CHECKS = 0").executeUpdate();
        coupleMissionProgressRepository.deleteAll();
        coupleMissionRepository.deleteAll();
        couplePhotoRepository.deleteAll();
        coupleRepository.deleteAll();
        memberRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        missionScheduleRepository.deleteAll();
        photoRepository.deleteAll();
        em.createNativeQuery("SET FOREIGN_KEEY_CHECKS = 1").executeUpdate();
        System.out.println("✅ DB reset complete (missions preserved).");
    }
}

