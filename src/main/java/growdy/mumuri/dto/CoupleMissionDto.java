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
    public static CoupleMissionDto from(CoupleMission cm, Long userId) {

        List<UserProgressDto> ps = cm.getProgresses().stream()
                .map(p -> new UserProgressDto(
                        p.getUserId(),
                        p.getStatus(),
                        p.getPhotoUrl(),
                        p.getCompletedAt()
                ))
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
