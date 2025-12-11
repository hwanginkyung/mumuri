package growdy.mumuri.service;

import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.HomeDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MainService {
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    public HomeDto getHome(CustomUserDetails user) {
        // 1. 로그인한 유저 조회
        Member me = memberRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 2. 커플 조회 (나를 member1 / member2 둘 다에서 찾아봄)
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(me.getId(), me.getId())
                .orElseThrow(() -> new IllegalStateException("아직 커플 매칭이 안 되어있습니다."));

        // 3. 상대방 찾기
        Member partner = couple.getMember1().getId().equals(me.getId())
                ? couple.getMember2()
                : couple.getMember1();

        // 4. 기념일 (Member에 anniversary 있다고 가정)
        LocalDate anniversary = me.getAnniversary(); // 혹시 Couple에 있다면 couple.getAnniversary()로 바꿔도 됨

        // 5. D+일 계산
        Integer dDay = null;
        if (anniversary != null) {
            long days = ChronoUnit.DAYS.between(anniversary, LocalDate.now());
            dDay = (int) days + 1;   // 기념일 당일을 D+1로 보고 싶으면 +1, D+0이면 +0
        }

        // 6. 커플 미션 (도메인에 맞게 getter 이름만 맞춰주면 됨)
        // 예시: Couple에 현재 미션이 들어있다고 가정
        List<CoupleMission> mission = couple.getQuestions(); // 실제 필드/메서드명에 맞게 수정

        // 7. 채팅방 ID 조회 (커플 기준으로 방 하나 있다고 가정)
        ChatRoom room = chatRoomRepository.findByCouple(couple)
                .orElseThrow(() -> new IllegalStateException("채팅방이 존재하지 않습니다."));
        Long roomId = room.getId();

        // 8. DTO 조립
        HomeDto dto = new HomeDto(
                anniversary,
                partner.getName(),   // 닉네임 쓰고 싶으면 getNickname()으로 변경
                mission,
                dDay,
                roomId
        );
        return dto;
    }
}
