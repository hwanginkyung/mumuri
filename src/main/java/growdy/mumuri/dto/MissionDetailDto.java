package growdy.mumuri.dto;

import growdy.mumuri.domain.MissionOwnerType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MissionDetailDto(
        Long photoId,
        MissionOwnerType ownerType,
        String ownerNickname,
        LocalDateTime createdAt,
        String imageUrl,      // presigned URL
        String missionText    // Photo.description 사용
) {}
