package growdy.mumuri.dto;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.MissionOwnerType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HomeDto(
        LocalDate anniversary,
        Integer dDay,
        Long coupleId,
        Long roomId,
        Integer missionCompletedCount,
        MainPhotoDto mainPhoto,
        String myProfileImageUrl,
        String partnerProfileImageUrl,
        String myName,
        String partnerName
) {
        public record MainPhotoDto(
                Long photoId,
                String imageUrl,              // presigned
                MissionOwnerType uploaderType, // ME / PARTNER
                String uploaderNickname,
                LocalDateTime createdAt
        ) {}
}