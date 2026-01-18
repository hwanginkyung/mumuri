package growdy.mumuri.login.service;

import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.Photo;
import growdy.mumuri.dto.RegisterResult;
import growdy.mumuri.login.dto.AppleUserInfo;
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
    private final ScheduleRepository scheduleRepository;
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
     * Apple 간편로그인 시, 기존 회원이면 그대로 반환, 없으면 신규 생성
     */
    @Transactional
    public RegisterResult registerIfAbsent(AppleUserInfo userInfo) {
        String appleId = userInfo.id();
        if (appleId == null || appleId.isBlank()) {
            throw new IllegalArgumentException("Apple user id is missing");
        }

        Optional<Member> existingUser = memberRepository.findByAppleId(appleId);
        if (existingUser.isPresent()) {
            System.out.println("기존 사용자 로그인(Apple): " + existingUser.get());
            return new RegisterResult(existingUser.get(), false);
        }

        Member newUser = new Member();
        newUser.setAppleId(appleId);
        newUser.setEmail(userInfo.email());
        newUser.setNickname(resolveAppleNickname(userInfo));
        newUser.setStatus("solo");
        newUser.setDeleted(false);

        Member saved = memberRepository.save(newUser);
        System.out.println("새 회원 저장됨(Apple): " + saved);
        return new RegisterResult(saved, true);
    }

    private String resolveAppleNickname(AppleUserInfo userInfo) {
        if (userInfo.name() != null && !userInfo.name().isBlank()) {
            return userInfo.name();
        }
        if (userInfo.email() != null && !userInfo.email().isBlank()) {
            int atIndex = userInfo.email().indexOf('@');
            return atIndex > 0 ? userInfo.email().substring(0, atIndex) : userInfo.email();
        }
        return "AppleUser";
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

        // 1) 커플이면 커플 및 관련 데이터 삭제 + partner solo 처리
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElse(null);

        if (couple != null) {
            Long coupleId = couple.getId();

            Member m1 = couple.getMember1();
            Member m2 = couple.getMember2();
            Member partner = null;

            if (m1 != null && m1.getId().equals(memberId)) partner = m2;
            else if (m2 != null && m2.getId().equals(memberId)) partner = m1;

            // ✅ (중요) 커플 삭제 전에 양쪽 member가 들고 있는 couple 참조를 끊어줘야 flush 때 안터짐
            if (m1 != null) m1.setCouple(null);
            if (m2 != null) m2.setCouple(null);
            member.setCouple(null);

            // mainPhoto가 커플 사진을 참조 중이면, 사진 삭제 시 FK/flush 이슈 생길 수 있어서 미리 끊기
            member.setMainPhoto(null);
            if (partner != null) partner.setMainPhoto(null);

            // partner는 솔로로 복구
            if (partner != null) {
                partner.setStatus("solo");
                partner.setCoupleCode(null);
                partner.setAnniversary(null);
                partner.setCouple(null);
            }

            // member도 커플 정보 끊기
            member.setCoupleCode(null);
            member.setAnniversary(null);

            // partner 변경사항 먼저 flush (삭제 전에 영속성 컨텍스트 안정화)
            if (partner != null) memberRepository.save(partner);
            memberRepository.save(member);
            memberRepository.flush();

            // (선택이지만 권장) 커플 일정이 couple FK 물고 있어서 커플 삭제 전에 삭제
            scheduleRepository.deleteByCoupleId(coupleId);

            // 채팅방 + 채팅 메시지 삭제
            ChatRoom room = chatRoomRepository.findByCouple(couple).orElse(null);
            if (room != null) {
                chatMessageRepository.deleteByChatRoomId(room.getId());
                chatRoomRepository.delete(room);
            }

            // 미션/프로그레스 삭제
            List<Long> cmIds = coupleMissionRepository.findIdsByCoupleId(coupleId);
            if (!cmIds.isEmpty()) {
                coupleMissionProgressRepository.deleteByCoupleMissionIdIn(cmIds);
            }
            coupleMissionRepository.deleteByCoupleId(coupleId);

            // 사진 삭제
            photoRepository.deleteByCoupleId(coupleId);

            // 커플 삭제
            coupleRepository.delete(couple);
        }

        // 2) 탈퇴 처리 (soft delete)
        member.setDeleted(true);
        member.setStatus("deleted");
        member.setNickname("탈퇴한 사용자");
        member.setEmail("deleted_" + memberId + "@mumuri.invalid");
        member.setKakaoId(null);
        member.setAppleId(null);
        member.setProfileImageKey(null);
        member.setMainPhoto(null);
        member.setCoupleCode(null);
        member.setAnniversary(null);
        member.setCouple(null);

        memberRepository.save(member);
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
