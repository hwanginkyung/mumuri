package growdy.mumuri.dto;

import growdy.mumuri.domain.ScheduleOwnerType;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        boolean couple,
        ScheduleOwnerType ownerType
) {}
