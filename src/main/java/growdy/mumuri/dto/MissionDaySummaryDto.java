package growdy.mumuri.dto;

import java.time.LocalDate;

public record MissionDaySummaryDto(
        LocalDate date,
        boolean hasPhoto,     // 그 날 커플 미션 사진이 1장 이상 있으면 true
        String thumbnailUrl   // 대표 썸네일(아무거나 한 장)
) {}
