package growdy.mumuri.dto;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.CoupleMissionProgress;
import growdy.mumuri.domain.MissionStatus;
import growdy.mumuri.domain.ProgressStatus;

import java.time.Instant;
import java.util.Comparator;

public record CoupleMissionHistoryDto(
        Long missionId,
        String title,
        String photoUrl,
        MissionStatus status,
        Instant completedAt
) {

    public static CoupleMissionHistoryDto from(CoupleMission cm) {
        MissionStatus status = cm.getStatus();

        String photoUrl = null;
        Instant completedAt = null;

        if (status == MissionStatus.COMPLETED) {
            // COMPLETED인 경우: CoupleMission.completedAt 기준으로 정렬용 시간 사용
            completedAt = cm.getCompletedAt();

            // 두 사람 중 더 늦게 완료한 progress의 사진 사용
            CoupleMissionProgress latest = cm.getProgresses().stream()
                    .filter(p -> p.getCompletedAt() != null)
                    .max(Comparator.comparing(CoupleMissionProgress::getCompletedAt))
                    .orElse(null);

            if (latest != null) {
                photoUrl = latest.getPhotoUrl();
            }

        } else if (status == MissionStatus.HALF_DONE) {
            // HALF_DONE인 경우: DONE인 progress 하나의 completedAt / photoUrl 사용
            CoupleMissionProgress done = cm.getProgresses().stream()
                    .filter(p -> p.getStatus() == ProgressStatus.DONE)
                    .findFirst()
                    .orElse(null);

            if (done != null) {
                completedAt = done.getCompletedAt();
                photoUrl = done.getPhotoUrl();
            }
        }

        return new CoupleMissionHistoryDto(
                cm.getId(),                     // <- 여기서 CoupleMission.id 를 missionId로 사용
                cm.getMission().getTitle(),     // Mission의 title
                photoUrl,
                status,
                completedAt
        );
    }
}
