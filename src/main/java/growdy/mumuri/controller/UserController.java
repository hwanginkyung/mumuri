package growdy.mumuri.controller;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.CoupleMatchDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.service.CoupleService;
import growdy.mumuri.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserSettingService userSettingService;
    private final CoupleService coupleService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @PostMapping("/name")
    public ResponseEntity<String> UpdateName(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam String name) {
        Long memberId= user.getId();
        userSettingService.updateMemberName(memberId, name);
        return ResponseEntity.ok("Name updated successfully");
    }
    @PostMapping("/birthday")
    public ResponseEntity<String> UpdateBirthday(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam LocalDate birthday) {
        Long memberId= user.getId();
        userSettingService.updateMemberBirthday(memberId, birthday);
        return ResponseEntity.ok("Birthday updated successfully");
    }
    @PostMapping("/anniversary")
    public String UpdateAnniversary(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam LocalDate anniversary) {
        Long memberId= user.getId();
        Member member = memberRepository.findById(user.getId()).orElse(null);
        userSettingService.updateMemberAnniversary(memberId, anniversary);
        memberService.makeCoupleCode(memberId);
        return member.getCoupleCode();
    }
    @PostMapping("/couple")
    public ResponseEntity<CoupleMatchDto> MakeCouple(@AuthenticationPrincipal CustomUserDetails user,
                                                      @RequestParam String coupleCode) {
        coupleService.checkAndSetCouple(user.getId(), coupleCode);
        return ResponseEntity.ok(new CoupleMatchDto("Couple matched successfully", user.getUsername()));
    }
    @GetMapping("/couple/already")
    public ResponseEntity<String> CheckCouple(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(coupleService.check(user.getUser()));
    }
}
