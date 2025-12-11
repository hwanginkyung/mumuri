package growdy.mumuri.dto;

import java.time.LocalDate;

public record MyPageDto(
        String name,
        LocalDate birthday,
        LocalDate anniversary,
        LocalDate birthdayCouple,
        Integer dDay){}