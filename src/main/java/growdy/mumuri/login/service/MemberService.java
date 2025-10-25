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
        Long kakaoId = Long.valueOf(userInfo.id());

        // 1️⃣ 카카오 ID로 먼저 회원 조회
        Optional<Member> existingUser = memberRepository.findByKakaoId(kakaoId);
        if (existingUser.isPresent()) {
            System.out.println("기존 사용자 로그인: " + existingUser.get());
            return existingUser.get();
        }

        // 2️⃣ 신규 회원 등록
        Member newUser = new Member();
        newUser.setKakaoId(kakaoId);
        newUser.setEmail(userInfo.email());
        newUser.setNickname(userInfo.nickname());
        newUser.setStatus("solo");

        Member saved = memberRepository.save(newUser);
        System.out.println("새 회원 저장됨: " + saved);
        return saved;
    }
    public void makeCoupleCode(Long userId){
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
