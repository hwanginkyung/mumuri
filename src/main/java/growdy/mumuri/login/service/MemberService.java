package growdy.mumuri.login.service;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;

    public Member registerIfAbsent(KakaoUserInfo userInfo) {
        Long ids = Long.valueOf(userInfo.id());
        Optional<Member> existingUser = memberRepository.findById(ids);
        System.out.println("조회된 사용자: " + existingUser);
        int atIndex = userInfo.email().indexOf("@");
        String spotNickname = (atIndex != -1) ? userInfo.email().substring(0, atIndex) : userInfo.email();
        return memberRepository.findById(ids)
                .orElseGet(() -> {
                    Member newUser = new Member();
                    newUser.setEmail(userInfo.email());
                    newUser.setNickname(userInfo.nickname());
                    Member saved = memberRepository.save(newUser);
                    System.out.println("새 회원 저장됨: " + saved);
                    return saved;
                });
    }
    public void setCoupleCode(Long userId){
        Member member = memberRepository.findById(userId).orElse(null);
        String code;
        do {
            code = generateCoupleCode();
        } while (memberRepository.existsByCoupleCode(code)); // 중복 방지 체크

        member.setCoupleCode(code);

    }
    public String generateCoupleCode() {
        int length = 8;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

}
