package growdy.mumuri.dto;

import growdy.mumuri.domain.CoupleMission;

import java.time.LocalDate;
import java.util.List;

public record HomeDto (
        LocalDate anniversary,
        String name,
        List<CoupleMission> coupleMission,
        Integer date,
        Long roomId
        )
{}
