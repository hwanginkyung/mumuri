package growdy.mumuri.dto;

import growdy.mumuri.domain.ProgressStatus;

public record UserProgressDto(Long userId, ProgressStatus status, String photoUrl) {}
