package growdy.mumuri.dto;

import growdy.mumuri.domain.CoupleMission;

import java.time.LocalDate;
import java.util.List;

public record HomeDto (
        LocalDate anniversary,
        Long coupleId,
        String name,
        List<MissionSummaryDto> coupleMission,
        Integer date,
        Long roomId
        )
{}
