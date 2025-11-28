package growdy.mumuri.dto;

import growdy.mumuri.domain.ProgressStatus;

import java.time.Instant;

public record UserProgressDto(Long userId, ProgressStatus status, String photoUrl, Instant completedAt) {}
