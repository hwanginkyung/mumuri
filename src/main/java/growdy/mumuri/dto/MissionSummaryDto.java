package growdy.mumuri.dto;

import growdy.mumuri.domain.MissionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MissionSummaryDto {
    private Long id;
    private String title;
    private MissionStatus status;
}