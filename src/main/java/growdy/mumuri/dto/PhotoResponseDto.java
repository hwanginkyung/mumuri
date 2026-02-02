package growdy.mumuri.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PhotoResponseDto {
    private Long id;
    private String presignedUrl; // 프론트에서 바로 <img src> 가능
    private Long uploadedBy;
    private LocalDateTime createdAt;
    private boolean blurred;
    private String blurMessage;
}
