package growdy.mumuri;


import growdy.mumuri.login.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
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
    @PostConstruct
    @Transactional
    public void resetDatabaseExceptMissions() {
        System.out.println("🧹 Initializing DB... Deleting all except mission table.");

        coupleMissionProgressRepository.deleteAll();
        coupleMissionRepository.deleteAll();
        couplePhotoRepository.deleteAll();
        coupleRepository.deleteAll();
        memberRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        missionScheduleRepository.deleteAll();
        photoRepository.deleteAll();

        System.out.println("✅ DB reset complete (missions preserved).");
    }
}

