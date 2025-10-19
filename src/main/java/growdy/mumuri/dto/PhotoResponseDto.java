package growdy.mumuri.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhotoResponseDto {
    private Long id;
    private String presignedUrl; // 프론트에서 바로 <img src> 가능
    private Long uploadedBy;
}
