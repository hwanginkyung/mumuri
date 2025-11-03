package growdy.mumuri.controller;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.CoupleMatchDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.service.CoupleService;
import growdy.mumuri.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {
    private final UserSettingService userSettingService;
    private final CoupleService coupleService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    @PostMapping("/name")
    public ResponseEntity<String> UpdateName(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam String name) {
        Long memberId= user.getId();
        userSettingService.updateMemberName(memberId, name);
        return ResponseEntity.ok(name);
    }

    @PostMapping("/birthday")
    public ResponseEntity<String> UpdateBirthday(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam LocalDate birthday) {
        Long memberId= user.getId();
        userSettingService.updateMemberBirthday(memberId, birthday);
        return ResponseEntity.ok(birthday.toString());
    }
    @PostMapping("/anniversary")
    public ResponseEntity<String> UpdateAnniversary(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam LocalDate anniversary) {
        Long memberId= user.getId();
        Member member = memberRepository.findById(user.getId()).orElse(null);
        userSettingService.updateMemberAnniversary(memberId, anniversary);
        memberService.makeCoupleCode(memberId);
        log.info("couplecodeby annivrsary : {}", member.getCoupleCode() );
        return ResponseEntity.ok(member.getCoupleCode());
    }
    @GetMapping("/coupletest")
    public String coupleTest(@AuthenticationPrincipal CustomUserDetails user){
        Member member = memberRepository.findById(user.getId()).orElse(null);
        log.info("couplecode : {}", member.getCoupleCode() );
        return member.getCoupleCode();
    }
    @PostMapping("/couple")
    public ResponseEntity<CoupleMatchDto> MakeCouple(@AuthenticationPrincipal CustomUserDetails user,
                                                      @RequestParam String coupleCode) {
        coupleService.checkAndSetCouple(user.getId(), coupleCode);
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(user.getId(),user.getId()).orElse(null);
        return ResponseEntity.ok(new CoupleMatchDto("Couple matched successfully", couple.getId()));
    }
    @GetMapping("/couple/already")
    public ResponseEntity<String> CheckCouple(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(coupleService.check(user.getUser()));
    }
    @GetMapping("/getuser")
    public Long getUser(@AuthenticationPrincipal CustomUserDetails user) {
        return user.getId();
    }
}
