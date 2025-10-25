package growdy.mumuri.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadStatusDto {
    private Long userId;
    private Long roomId;
}
