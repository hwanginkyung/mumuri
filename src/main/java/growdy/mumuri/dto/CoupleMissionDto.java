package growdy.mumuri.dto;

import growdy.mumuri.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CoupleMissionDto(
        Long missionId,
        String title,
        String description,
        MissionDifficulty difficulty,
        int reward,
        MissionStatus status,
        LocalDate missionDate,
        List<UserProgressDto> progresses,
        boolean myDone,          // ← 내가 완료했는가
        Instant myCompletedAt    // ← 내가 완료한 시간
){
    public static CoupleMissionDto from(CoupleMission cm, Long userId) {

        List<UserProgressDto> ps = cm.getProgresses().stream()
                .map(p -> new UserProgressDto(
                        p.getUserId(),
                        p.getStatus(),
                        p.getPhotoUrl(),
                        p.getCompletedAt()
                ))
                .toList();

        // 🔥 여기에서 내가 한 progress 찾는다
        CoupleMissionProgress myProgress = cm.getProgresses().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        boolean myDone = myProgress != null && myProgress.getStatus() == ProgressStatus.DONE;
        Instant myTime = myProgress != null ? myProgress.getCompletedAt() : null;

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
                myDone,          // 내가 완료했는지
                myTime           // 완료시간
        );
    }
}
