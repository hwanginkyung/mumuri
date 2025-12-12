package growdy.mumuri.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleCreateRequest(
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean allDay,
        boolean couple // 커플 일정으로 등록 체크박스
) {}