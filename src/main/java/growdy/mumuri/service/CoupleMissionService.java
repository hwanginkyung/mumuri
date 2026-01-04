package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.*;
import growdy.mumuri.dto.CoupleMissionHistoryDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleMissionService {
    private final CoupleMissionRepository coupleMissionRepository;
    private final CoupleRepository coupleRepository;
    private final PhotoService photoService;
    private final S3Upload s3Upload;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private Couple getCouple(Long userId) {
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(userId, userId).orElseThrow();
        return couple;
    }

    @Transactional(readOnly = true)
    public List<CoupleMission> getTodayMissions(Long userId) {
        Couple couple = getCouple(userId);
        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<CoupleMission> missions = coupleMissionRepository.findTodayWithProgresses(
                couple.getId(), todayKst
        );

        // presigned URL로 교체
        missions.forEach(m -> m.getProgresses().forEach(p -> {
            String stored = p.getPhotoUrl();
            if (stored == null || stored.isEmpty()) return;
            if (stored.startsWith("http")) return;

            boolean shouldBlurForViewer =
                    m.getStatus() == MissionStatus.HALF_DONE
                            && p.getStatus() == ProgressStatus.DONE
                            && p.getUserId() != null
                            && !p.getUserId().equals(userId); // ✅ “상대방만” 블러

            String keyToExpose = shouldBlurForViewer ? toBlurKey(stored) : stored;
            p.setPhotoUrl(s3Upload.presignedGetUrl(keyToExpose, Duration.ofMinutes(10)));
        }));


        return missions;
    }

    @Transactional
    public Instant completeMyPart(Long userId, Long missionId, MultipartFile photoOrNull) {
        Couple couple = getCouple(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

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

        saveMissionChatLogs(couple, userId, cm, url);
        cm.updateStatusByProgress(); // COMPLETED 계산

        return cm.getCompletedAt();
    }
    @Transactional
    public Instant completeWithKey(Long userId, Long missionId, String fileKey) {
        Couple couple = getCouple(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

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

        // ✅ fileKey는 "couples/..." 같은 S3 key 여야 함
        String normalizedKey = normalizeImageKey(fileKey);

        progress.complete(normalizedKey);      // photoUrl에는 key 저장
        cm.updateStatusByProgress();           // status HALF_DONE/COMPLETED 확정

        saveMissionChatLogs(couple, userId, cm, normalizedKey);

        return cm.getCompletedAt();
    }
    @Transactional(readOnly = true)
    public List<CoupleMissionHistoryDto> getMissionHistory(Long userId) {
        Couple couple = getCouple(userId);

        List<MissionStatus> statuses = List.of(MissionStatus.HALF_DONE, MissionStatus.COMPLETED);

        List<CoupleMission> missions =
                coupleMissionRepository.findByCoupleIdAndStatusIn(couple.getId(), statuses);

        // ✅ 여기(가져온 직후)에 넣기
        applyPresignedUrls(missions, userId);

        return missions.stream()
                .map(CoupleMissionHistoryDto::from)
                .filter(dto -> dto.completedAt() != null)
                .sorted(Comparator.comparing(CoupleMissionHistoryDto::completedAt))
                .toList();
    }

    // ✅ getMissionHistory 아래 아무데나(보통 하단)에 추가
    private void applyPresignedUrls(List<CoupleMission> missions, Long viewerId) {
        missions.forEach(m -> m.getProgresses().forEach(p -> {
            String stored = p.getPhotoUrl();
            if (stored == null || stored.isEmpty()) return;
            if (stored.startsWith("http")) return;

            boolean shouldBlurForViewer =
                    m.getStatus() == MissionStatus.HALF_DONE
                            && p.getStatus() == ProgressStatus.DONE
                            && p.getUserId() != null
                            && viewerId != null
                            && !p.getUserId().equals(viewerId);

            String keyToExpose = shouldBlurForViewer ? toBlurKey(stored) : stored;
            p.setPhotoUrl(s3Upload.presignedGetUrl(keyToExpose, Duration.ofMinutes(10)));
        }));
    }

    private String normalizeImageKey(String value) {
        if (value == null || value.isBlank()) return null;

        // presigned GET/PUT url 같은 거면 금지(만료됨)
        if (value.startsWith("http") && (value.contains("X-Amz-Signature") || value.contains("X-Amz-Credential"))) {
            throw new IllegalArgumentException("presigned URL 말고 S3 key를 보내야 합니다.");
        }

        // 외부 http URL을 허용하고 싶다면 여기서 return value;
        // 하지만 너 케이스는 S3 key로 통일하는 게 제일 안정적이라 key만 허용 추천.
        if (value.startsWith("http")) {
            throw new IllegalArgumentException("http URL 말고 S3 key를 보내야 합니다.");
        }

        return value; // ✅ key
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
    private Long getPartnerId(Couple couple, Long myId) {
        if (couple.getMember1() != null && couple.getMember1().getId().equals(myId)) {
            return couple.getMember2() != null ? couple.getMember2().getId() : null;
        }
        if (couple.getMember2() != null && couple.getMember2().getId().equals(myId)) {
            return couple.getMember1() != null ? couple.getMember1().getId() : null;
        }
        return null;
    }
    private void saveMissionChatLogs(Couple couple, Long userId, CoupleMission cm, String imageKey) {
        ChatRoom room = chatRoomRepository.findByCouple(couple)
                .orElseThrow(() -> new IllegalStateException("채팅방이 없습니다."));

        Member performer = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        String title = cm.getMission().getTitle();

        // 1) 텍스트 로그
        chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(performer)
                .type(ChatMessageType.MISSION_TEXT)
                .missionHistoryId(cm.getId())   // ✅ 추가
                .message(title)
                .build());

        // 2) 이미지 로그
        if (imageKey != null && !imageKey.isBlank()) {
            chatMessageRepository.save(ChatMessage.builder()
                    .chatRoom(room)
                    .sender(performer)
                    .type(ChatMessageType.MISSION_IMAGE)
                    .missionHistoryId(cm.getId())   // ✅ 추가
                    .message("")
                    .imageUrl(imageKey)
                    .build());
        }
    }
    private String toBlurKey(String originalKey) {
        if (originalKey == null || originalKey.isBlank()) return originalKey;
        int idx = originalKey.lastIndexOf('/');
        if (idx < 0) return "blur_" + originalKey;
        return originalKey.substring(0, idx + 1) + "blur_" + originalKey.substring(idx + 1);
    }
}
