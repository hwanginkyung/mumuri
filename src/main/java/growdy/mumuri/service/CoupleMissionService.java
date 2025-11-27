package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.CoupleMissionProgress;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleMissionProgressRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleMissionService {
    private final CoupleMissionRepository coupleMissionRepository;
    private final CoupleMissionProgressRepository progressRepository;
    private final CoupleRepository coupleRepository;
    private final PhotoService photoService;
    private Couple getCouple(Long userId) {
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(userId, userId).orElseThrow();
        return couple;
    }

    @Transactional(readOnly = true)
    public List<CoupleMission> getTodayMissions(Long userId) {
        Couple couple = getCouple(userId);
        return coupleMissionRepository.findTodayWithProgresses(couple.getId(), LocalDate.now());
    }

    @Transactional
    public Instant completeMyPart(Long userId, Long missionId, MultipartFile photoOrNull) {
        Couple couple = getCouple(userId);
        LocalDate today = LocalDate.now();

        CoupleMission cm = coupleMissionRepository
                .findTodayWithProgresses(couple.getId(), today)
                .stream()
                .filter(c -> c.getMission().getId().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("오늘 미션이 아닙니다."));

        // progress 없으면 두 명 모두 생성
        if (cm.getProgresses().isEmpty()) {
            Long m1 = couple.getMember1().getId();
            Long m2 = couple.getMember2().getId();

            new CoupleMissionProgress(cm, m1);
            new CoupleMissionProgress(cm, m2);

            // cascade 때문에 cm 만 저장해도 progress 자동 저장됨
            coupleMissionRepository.save(cm);
        }

        // 내 progress 찾기
        CoupleMissionProgress progress = cm.getProgresses().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("progress 생성 실패"));

        // 사진 처리
        String url = "";
        if (photoOrNull != null && !photoOrNull.isEmpty()) {
            url = photoService.uploadPhoto(couple.getId(), photoOrNull, userId, missionId);
        }

        progress.complete(url);

        cm.updateStatusByProgress(); // COMPLETED 계산

        return cm.getCompletedAt();
    }

}
