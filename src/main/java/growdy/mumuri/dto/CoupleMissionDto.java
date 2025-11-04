package growdy.mumuri.dto;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.Mission;
import growdy.mumuri.domain.MissionDifficulty;
import growdy.mumuri.domain.MissionStatus;

import java.time.LocalDate;
import java.util.List;

public record CoupleMissionDto(
        Long missionId,
        String title,
        String description,
        MissionDifficulty difficulty,
        int reward,
        MissionStatus status,
        LocalDate missionDate,
        List<UserProgressDto> progresses
){
    public static CoupleMissionDto from(CoupleMission cm) {
        Mission m = cm.getMission();
        List<UserProgressDto> ps = cm.getProgresses().stream()
                .map(p -> new UserProgressDto(p.getUserId(), p.getStatus(), p.getPhotoUrl()))
                .toList();
        return new CoupleMissionDto(
                m.getId(), m.getTitle(), m.getDescription(), m.getDifficulty(),m.getReward(), cm.getStatus(), cm.getMissionDate(), ps
        );
    }
}