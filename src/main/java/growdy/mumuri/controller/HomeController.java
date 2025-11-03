package growdy.mumuri.controller;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.dto.MainDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.service.DdayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {
    private final DdayService   dayService;
    private final CoupleRepository coupleRepository;

    @GetMapping("/user/main")
    public MainDto mainDto(@AuthenticationPrincipal CustomUserDetails user){
        Couple couple= coupleRepository.findByMember1IdOrMember2Id(user.getId(),user.getId()).orElseThrow();
        long dday=dayService.getDday(couple.getId());
        MainDto mainDto = new MainDto();
        mainDto.setDday(dday);
        return mainDto;
    }
}
