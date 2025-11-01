package growdy.mumuri.service;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    @Transactional
    public void updateMemberName(Long memberId, String name) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setName(name);
    }
    @Transactional
    public void updateMemberBirthday(Long memberId, LocalDate birthday) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setBirthday(birthday);
    }
    @Transactional
    public void updateMemberAnniversary(Long memberId, LocalDate anniversary) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setAnniversary(anniversary);
        memberService.makeCoupleCode(memberId);
    }

}
