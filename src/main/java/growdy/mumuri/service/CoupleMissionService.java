package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.CoupleMissionProgress;
import growdy.mumuri.domain.ProgressStatus;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleMissionProgressRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
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
    private final S3Upload s3Upload;
    private Couple getCouple(Long userId) {
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(userId, userId).orElseThrow();
        return couple;
    }

    @Transactional(readOnly = true)
    public List<CoupleMission> getTodayMissions(Long userId) {
        Couple couple = getCouple(userId);
        List<CoupleMission> missions = coupleMissionRepository.findTodayWithProgresses(
                couple.getId(), LocalDate.now()
        );

        // presigned URL로 교체
        missions.forEach(m -> {
            m.getProgresses().forEach(p -> {
                if (p.getPhotoUrl() != null && !p.getPhotoUrl().isEmpty()) {
                    String presigned = s3Upload.presignedGetUrl(
                            p.getPhotoUrl(),
                            Duration.ofMinutes(10)
                    );
                    p.setPhotoUrl(presigned);
                }
            });
        });

        return missions;
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
    @Transactional
    public Instant completeWithUrl(Long userId, Long missionId, String fileUrl) {
        Couple couple = getCouple(userId);
        LocalDate today = LocalDate.now();

        CoupleMission cm = coupleMissionRepository
                .findTodayWithProgresses(couple.getId(), today)
                .stream()
                .filter(c -> c.getMission().getId().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("오늘 미션이 아닙니다."));

        // progress 없으면 생성
        if (cm.getProgresses().isEmpty()) {
            Long m1 = couple.getMember1().getId();
            Long m2 = couple.getMember2().getId();

            new CoupleMissionProgress(cm, m1);
            new CoupleMissionProgress(cm, m2);

            coupleMissionRepository.save(cm);
        }

        // 내 progress 찾기
        CoupleMissionProgress progress = cm.getProgresses().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();

        // 사진 URL 저장
        progress.complete(fileUrl);

        // 여기서 완료 시간(Instant) 다시 찍기
        Instant now = Instant.now();
        if (progress.getStatus() == ProgressStatus.DONE) {
            progress.setCompletedAt(now);
        }

        // 미션 전체 상태 업데이트
        cm.updateStatusByProgress();
        cm.setCompletedAt(now);  // 🔥 전체 미션 완료 시간도 기록

        return cm.getCompletedAt();
    }

}
