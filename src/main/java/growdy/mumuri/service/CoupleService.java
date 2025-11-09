package growdy.mumuri.service;


import growdy.mumuri.domain.*;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CoupleService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberService memberService;
    private final MissionRepository missionRepository;
    private final CoupleMissionRepository coupleMissionRepository;

    @Transactional
    public void checkAndSetCouple(Long userId, String coupleCode) {
        Member coupleMember = memberRepository
                .findByCoupleCodeAndStatusAndIdNot(coupleCode, "solo", userId)
                .orElseThrow(() -> new IllegalArgumentException("No matching couple found"));

        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Couple 생성
        Couple couple = new Couple();
        couple.setMember1(user);
        couple.setMember2(coupleMember);
        couple.setCoupleCode(coupleCode);
        couple.setAnniversary(user.getAnniversary());
        coupleRepository.save(couple);
        ChatRoom chatRoom = new ChatRoom(couple);
        chatRoomRepository.save(chatRoom);
        // 상태 변경
        user.setStatus("couple");
        coupleMember.setStatus("couple");
        user.setCoupleCode(coupleCode);
        coupleMember.setCoupleCode(coupleCode);
        List<Mission> allMissions = missionRepository.findByActiveTrue();

        Collections.shuffle(allMissions);
        List<Mission> selected = allMissions.stream()
                .limit(6)
                .toList();

        // 4️⃣ CoupleMission 엔티티 생성 후 저장
        LocalDate today = LocalDate.now();
        selected.forEach(m -> {
            CoupleMission cm = new CoupleMission(couple, m, today);
            coupleMissionRepository.save(cm);
        });
    }
    public String check(Member user){
        Couple couple = user.getCouple();
        if (couple == null) {
            return "not couple";
        }
        if(!couple.getCoupleCode().equals(user.getCoupleCode())){
            return "not couple";
        }
        return "couple";
    }
    @Transactional
    public Couple test(Member user){
        // Couple 생성
        Member member = new Member();
        memberRepository.save(member);
        member.setCoupleCode(user.getCoupleCode());
        user.setStatus("couple");
        Couple couple = new Couple();
        couple.setAnniversary(user.getAnniversary());
        couple.setMember1(member);
        couple.setMember2(user);
        couple.setCoupleCode(user.getCoupleCode());
        coupleRepository.save(couple);
        ChatRoom chatRoom = new ChatRoom(couple);
        chatRoomRepository.save(chatRoom);
        return couple;
    }
    @Transactional
    public String newcode (Member user){
        // 미리 등록
        Member member = new Member();
        memberRepository.save(member);
        memberService.makeCoupleCode(member.getId());
        memberRepository.save(member);
        return member.getCoupleCode();
    }

}
