package growdy.mumuri.dto;

import growdy.mumuri.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record CoupleMissionDto(
        Long missionId,
        String title,
        String description,
        MissionDifficulty difficulty,
        int reward,
        MissionStatus status,
        LocalDate missionDate,
        List<UserProgressDto> progresses,
        boolean myDone,
        Instant myCompletedAt
) {
    private static final String BLUR_MESSAGE = "상대방이 보낸 사진을 보려면 먼저 같은 미션을 수행해야 해요.";
    public static CoupleMissionDto from(CoupleMission cm, Long userId) {

        List<UserProgressDto> ps = cm.getProgresses().stream()
                .map(p -> {
                    boolean shouldBlurForViewer =
                            cm.getStatus() == MissionStatus.HALF_DONE
                                    && p.getStatus() == ProgressStatus.DONE
                                    && p.getUserId() != null
                                    && !p.getUserId().equals(userId);

                    return new UserProgressDto(
                            p.getUserId(),
                            p.getStatus(),
                            p.getPhotoUrl(),
                            p.getCompletedAt(),
                            shouldBlurForViewer,
                            shouldBlurForViewer ? BLUR_MESSAGE : null
                    );
                })
                .toList();

        Optional<CoupleMissionProgress> myProgressOpt = cm.getProgresses().stream()
                .filter(p -> p.getUserId() != null && p.getUserId().equals(userId))
                .findFirst();

        boolean myDone = myProgressOpt
                .map(p -> p.getStatus() == ProgressStatus.DONE)
                .orElse(false);

        Instant myTime = myProgressOpt
                .map(CoupleMissionProgress::getCompletedAt)
                .orElse(null);

        Mission m = cm.getMission();

        return new CoupleMissionDto(
                m.getId(),
                m.getTitle(),
                m.getDescription(),
                m.getDifficulty(),
                m.getReward(),
                cm.getStatus(),
                cm.getMissionDate(),
                ps,
                myDone,
                myTime
        );
    }
}
