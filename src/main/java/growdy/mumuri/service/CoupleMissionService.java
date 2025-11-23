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

        CoupleMission cm = coupleMissionRepository.findTodayWithProgresses(couple.getId(), today).stream()
                .filter(c -> c.getMission().getId().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("오늘 미션이 아닙니다."));

        CoupleMissionProgress progress = progressRepository
                .findByCoupleMissionIdAndUserId(cm.getId(), userId)
                .orElseGet(() -> new CoupleMissionProgress(cm, userId));
        String url="";
        if (photoOrNull != null && !photoOrNull.isEmpty()) {
            // S3 업로드 + S3 URL 생성

            url=photoService.uploadPhoto(couple.getId(),photoOrNull,userId,missionId);
        }

        progress.complete(url);
        progressRepository.save(progress);
        cm.updateStatusByProgress();
        return cm.getCompletedAt();
    }
}
