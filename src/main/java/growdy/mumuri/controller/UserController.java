package growdy.mumuri.controller;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.Test;
import growdy.mumuri.login.AuthGuard;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

    @PostMapping("/test")
    @Transactional
    public void test(){
        Test testss=new Test();
        testss.setName("testss");


    }

    @PostMapping("/name")
    public ResponseEntity<String> UpdateName(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam String name) {
        Long memberId= AuthGuard.requireUser(user).getId();
        userSettingService.updateMemberName(memberId, name);
        return ResponseEntity.ok(name);
    }

    @PostMapping("/birthday")
    public ResponseEntity<String> UpdateBirthday(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam(required = false) String birthday) {
        Long memberId= AuthGuard.requireUser(user).getId();
        LocalDate parsedBirthday = StringUtils.hasText(birthday) ? LocalDate.parse(birthday) : null;
        userSettingService.updateMemberBirthday(memberId, parsedBirthday);
        return ResponseEntity.ok(parsedBirthday != null ? parsedBirthday.toString() : null);
    }
    @PostMapping("/anniversary")
    public ResponseEntity<String> updateAnniversary(@AuthenticationPrincipal CustomUserDetails user,
                                                    @RequestParam LocalDate anniversary) {
        Long memberId = AuthGuard.requireUser(user).getId();
        userSettingService.updateMemberAnniversary(memberId, anniversary);
        Member member = memberRepository.findById(memberId).orElse(null);
        String code= member.getCoupleCode();
        return ResponseEntity.ok(code);
    }

    /**
     * ✅ 내 커플 코드 조회 (없으면 생성해서 반환)
     *    - 마이페이지에서 "내 코드 보기" 버튼용
     *    - 옵션1, 옵션2 둘 다 여기 쓰면 됨
     */
    @GetMapping("/couple/code")
    public ResponseEntity<String> getOrCreateCoupleCode(@AuthenticationPrincipal CustomUserDetails user) {
        Member member = memberRepository.findById(AuthGuard.requireUser(user).getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getCoupleCode() == null) {
            memberService.makeCoupleCode(member.getId());
            log.info("generated coupleCode for memberId={}", member.getId());
        }

        log.info("coupleCode : {}", member.getCoupleCode());
        return ResponseEntity.ok(member.getCoupleCode());
    }

    @GetMapping("/coupletest")
    public String coupleTest(@AuthenticationPrincipal CustomUserDetails user){
        Member member = memberRepository.findById(AuthGuard.requireUser(user).getId()).orElse(null);
        log.info("couplecode : {}", member.getCoupleCode() );
        return member.getCoupleCode();
    }
    @PostMapping("/couple")
    public ResponseEntity<CoupleMatchDto> makeCouple(@AuthenticationPrincipal CustomUserDetails user,
                                                     @RequestParam String coupleCode) {
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        coupleService.checkAndSetCouple(authenticatedUser.getId(), coupleCode);
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(authenticatedUser.getId(), authenticatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("Couple not found after match"));

        return ResponseEntity.ok(new CoupleMatchDto("Couple matched successfully", couple.getId()));
    }
    @GetMapping("/couple/already")
    public ResponseEntity<String> CheckCouple(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(coupleService.check(AuthGuard.requireUser(user).getUser()));
    }
    @GetMapping("/getuser")
    public Long getUser(@AuthenticationPrincipal CustomUserDetails user) {
        return AuthGuard.requireUser(user).getId();
    }
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserDetails user) {

        Long memberId = AuthGuard.requireUser(user).getId();
        memberService.withdraw(memberId);

        // 프론트에서는 이 응답 받으면
        // - 로컬/쿠키 토큰 삭제
        // - 로그인 화면으로 이동
        return ResponseEntity.noContent().build();  // 204
    }
}
