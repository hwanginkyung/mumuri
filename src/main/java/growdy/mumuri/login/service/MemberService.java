package growdy.mumuri.login.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.RegisterResult;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    // private final AuthService authService; // refresh 토큰 날리고 싶으면 주입해서 사용

    /**
     * 카카오 간편로그인 시, 기존 회원이면 그대로 반환, 없으면 신규 생성
     */
    @Transactional
    public RegisterResult registerIfAbsent(KakaoUserInfo userInfo) {
        Long kakaoId = Long.valueOf(userInfo.id());

        // 1️⃣ 카카오 ID로 먼저 회원 조회
        Optional<Member> existingUser = memberRepository.findByKakaoId(kakaoId);
        if (existingUser.isPresent()) {
            System.out.println("기존 사용자 로그인: " + existingUser.get());
            return new RegisterResult(existingUser.get(), false);
        }

        // 2️⃣ 신규 회원 등록
        Member newUser = new Member();
        newUser.setKakaoId(kakaoId);
        newUser.setEmail(userInfo.email());
        newUser.setNickname(userInfo.nickname());
        newUser.setStatus("solo");      // 기본 상태: 커플 연결 전
        newUser.setDeleted(false);      // soft delete 플래그 기본값

        Member saved = memberRepository.save(newUser);
        System.out.println("새 회원 저장됨: " + saved);
        return new RegisterResult(saved, true);
    }

    /**
     * ✅ 커플 코드 생성 (없을 때만 호출하는 걸 추천)
     *    - "/user/couple/code" 에서만 사용
     */
    @Transactional
    public void makeCoupleCode(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 이미 코드가 있으면 그대로 유지하도록 하고 싶으면:
        if (member.getCoupleCode() != null) {
            return;
        }

        String code;
        do {
            code = generateCoupleCode();
        } while (memberRepository.existsByCoupleCode(code)); // 중복 방지

        member.setCoupleCode(code);
        // @Transactional + 영속 엔티티라면 save() 안 해도 flush 됨, 그래도 명시하고 싶으면 놔둬도 OK
        // memberRepository.save(member);
    }

    private String generateCoupleCode() {
        int length = 8;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    /**
     * ✅ 회원 탈퇴 (soft delete)
     *    - 커플 관계 정리
     *    - 회원 정보 비식별화
     *    - deleted 플래그 true
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 1. 커플 관계 정리
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElse(null);

        if (couple != null) {
            // 탈퇴한 사람 제거
            if (couple.getMember1() != null && couple.getMember1().getId().equals(memberId)) {
                couple.setMember1(null);
            } else if (couple.getMember2() != null && couple.getMember2().getId().equals(memberId)) {
                couple.setMember2(null);
            }

            // 둘 다 없어졌으면 커플 자체 삭제 (선택)
            if (couple.getMember1() == null && couple.getMember2() == null) {
                coupleRepository.delete(couple);
            }
        }

        // 2. 토큰 무효화 (refresh 사용 시)
        // authService.revokeAllForMember(memberId);

        // 3. soft delete 플래그 + 비식별 처리
        member.setDeleted(true);
        member.setStatus("deleted");
        member.setNickname("탈퇴한 사용자");
        member.setEmail(null);
        member.setKakaoId(null); // 재가입 허용 정책에 따라 유지/삭제 선택

        // 필요하면 기타 프로필도 초기화
        /*member.setCoupleCode(null);
        member.setAnniversary(null);
        member.setBirthday(null);*/
    }
}
