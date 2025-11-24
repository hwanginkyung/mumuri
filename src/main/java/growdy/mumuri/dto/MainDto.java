package growdy.mumuri.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MainDto {
    private long dday;
    private List<String> missionLists;

}
