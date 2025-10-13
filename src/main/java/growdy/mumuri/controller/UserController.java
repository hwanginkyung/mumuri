package growdy.mumuri.controller;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.CoupleCodeDto;
import growdy.mumuri.login.dto.CoupleMatchDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.service.CoupleService;
import growdy.mumuri.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> UpdateAnniversary(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam LocalDate anniversary) {
        Long memberId= user.getId();
        userSettingService.updateMemberBirthday(memberId, anniversary);
        return ResponseEntity.ok("Anniversary updated successfully");
    }
    @PostMapping("/couple")
    public ResponseEntity<CoupleMatchDto> CheckCouple(@AuthenticationPrincipal CustomUserDetails user,
                                                      @RequestParam String coupleCode) {
        coupleService.checkAndSetCouple(user.getId(), coupleCode);
        return ResponseEntity.ok(new CoupleMatchDto("Couple matched successfully", user.getUsername()));
    }
    @PostMapping("/getCode")
    public ResponseEntity<CoupleCodeDto> getCoupleCode(@AuthenticationPrincipal CustomUserDetails user){
        memberService.setCoupleCode(user.getId());
        Member member = memberRepository.findById(user.getId()).orElse(null);
        return ResponseEntity.ok(new CoupleCodeDto ("Couple code set successfully", member.getCoupleCode()));
    }

}
