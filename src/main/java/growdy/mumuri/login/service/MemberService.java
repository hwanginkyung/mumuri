package growdy.mumuri.login.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.Photo;
import growdy.mumuri.dto.RegisterResult;
import growdy.mumuri.login.dto.KakaoUserInfo;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final PhotoRepository photoRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CoupleMissionRepository coupleMissionRepository;
    private final CoupleMissionProgressRepository coupleMissionProgressRepository;
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

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElse(null);

        if (couple != null) {
            Long coupleId = couple.getId();

            // ✅ 상대방 찾기
            Member partner = null;
            if (couple.getMember1() != null && couple.getMember1().getId().equals(memberId)) {
                partner = couple.getMember2();
            } else if (couple.getMember2() != null && couple.getMember2().getId().equals(memberId)) {
                partner = couple.getMember1();
            }

            // =========================
            // 0) FK 끊기 (중요!!)
            // =========================
            member.setMainPhoto(null);
            if (partner != null) partner.setMainPhoto(null);

            // member 테이블 업데이트가 먼저 나가도록 flush (FK 때문에 중요)
            memberRepository.flush();

            // =========================
            // 1) 채팅방/메시지 삭제
            // =========================
            chatRoomRepository.findByCouple(couple).ifPresent(room -> {
                chatMessageRepository.deleteByChatRoomId(room.getId());
                chatRoomRepository.delete(room);
            });

            // =========================
            // 2) 미션 progress -> 미션 삭제
            // =========================
            List<Long> cmIds = coupleMissionRepository.findIdsByCoupleId(coupleId);
            if (!cmIds.isEmpty()) {
                coupleMissionProgressRepository.deleteByCoupleMissionIdIn(cmIds);
            }
            coupleMissionRepository.deleteByCoupleId(coupleId);

            // =========================
            // 3) 사진 삭제 (FK 끊은 뒤!)
            // =========================
            photoRepository.deleteByCoupleId(coupleId);

            // =========================
            // 4) couple 삭제
            // =========================
            coupleRepository.delete(couple);

            // ✅ 상대방은 솔로로 되돌리기 (추천)
            if (partner != null) {
                partner.setStatus("solo");
                partner.setCoupleCode(null);
                // partner.setAnniversary(null); // 정책에 따라 (커플 기념일을 member에 저장한다면 null 추천)
            }
        }

        // =========================
        // 5) 탈퇴자 소프트 탈퇴 처리
        // =========================
        member.setDeleted(true);
        member.setStatus("deleted");
        member.setNickname("탈퇴한 사용자");
        member.setCoupleCode(null);
        // member.setAnniversary(null); // 정책에 따라

        // email NOT NULL/UNIQUE 대비
        member.setEmail("deleted_" + memberId + "@mumuri.invalid");

        // 재가입 허용이면 null
        member.setKakaoId(null);

        member.setProfileImageKey(null);
    }

    @Transactional
    public void setMainPhoto(Long memberId, Long photoId) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        if (photo.isDeleted()) {
            throw new IllegalArgumentException("삭제된 사진입니다.");
        }

        // ✅ 최소 검증: 내 커플 사진인지(커플 앨범에서 고르는 거라면)
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(memberId, memberId)
                .orElseThrow(() -> new IllegalStateException("커플이 아닙니다."));

        if (!photo.getCouple().getId().equals(couple.getId())) {
            throw new IllegalArgumentException("내 커플의 사진이 아닙니다.");
        }

        me.setMainPhoto(photo);
    }
}
