package growdy.mumuri.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReadRequestDto {
    private Long roomId;
    private Long userId;
}