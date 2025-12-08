package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.*;
import growdy.mumuri.dto.CoupleMissionHistoryDto;
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
import java.util.Comparator;
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
    private final MemberRepository memberRepository;
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

        if (cm.getProgresses().isEmpty()) {
            Long m1 = couple.getMember1().getId();
            Long m2 = couple.getMember2().getId();

            new CoupleMissionProgress(cm, m1);
            new CoupleMissionProgress(cm, m2);

            coupleMissionRepository.save(cm);
        }

        CoupleMissionProgress progress = cm.getProgresses().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();

        // 1) 내 progress 완료 시간/사진은 여기서만 설정
        progress.complete(fileUrl);  // status=DONE, completedAt=Instant.now()

        // 2) 전체 미션 상태/완료 시간은 여기서만 설정
        cm.updateStatusByProgress(); // HALF_DONE이면 cm.completedAt=null, COMPLETED면 cm.completedAt=Instant.now()

        return cm.getCompletedAt();  // COMPLETED 아니면 null, COMPLETED면 완료 시간
    }

    @Transactional(readOnly = true)
    public List<CoupleMissionHistoryDto> getMissionHistory(Long userId) {

        Couple couple = getCouple(userId);

        List<MissionStatus> statuses = List.of(
                MissionStatus.HALF_DONE,
                MissionStatus.COMPLETED
        );

        List<CoupleMission> missions =
                coupleMissionRepository.findByCoupleIdAndStatusIn(couple.getId(), statuses);
        applyPresignedUrls(missions);

        return missions.stream()
                .map(CoupleMissionHistoryDto::from)
                .filter(dto -> dto.completedAt() != null)
                .sorted(Comparator.comparing(CoupleMissionHistoryDto::completedAt))
                .toList();
    }

    private void applyPresignedUrls(List<CoupleMission> missions) {
        missions.forEach(m -> m.getProgresses().forEach(p -> {
            String stored = p.getPhotoUrl();
            if (stored == null || stored.isEmpty()) return;

            // 이미 http로 시작하면 외부 URL이라고 보고 그대로 사용
            if (stored.startsWith("http")) return;

            // ✅ S3 key인 경우에만 presigned 만들기
            String presigned = s3Upload.presignedGetUrl(stored, Duration.ofMinutes(10));
            p.setPhotoUrl(presigned);
        }));
    }
}
