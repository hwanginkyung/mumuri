package growdy.mumuri.login.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CoupleCodeDto {
    private String message;
    private String coupleCode;
}