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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MainService {
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    public HomeDto getHome(CustomUserDetails user) {
        // 1. 로그인 유저
        Member me = memberRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 2. 커플 Optional 조회 (예외 던지지 않음)
        Optional<Couple> optionalCouple =
                coupleRepository.findByMember1IdOrMember2Id(me.getId(), me.getId());

        // 기본값들
        LocalDate anniversary = me.getAnniversary();
        Integer dDay = null;
        String partnerName = null;
        List<CoupleMission> missionList = null;
        Long roomId = null;

        // 커플이 있는 경우에만 계산
        if (optionalCouple.isPresent()) {
            Couple couple = optionalCouple.get();

            // partner
            Member partner =
                    couple.getMember1().getId().equals(me.getId()) ?
                            couple.getMember2() : couple.getMember1();

            if (partner != null) {
                partnerName = partner.getName();
            }

            // D+ 계산
            if (anniversary != null) {
                long days = ChronoUnit.DAYS.between(anniversary, LocalDate.now());
                dDay = (int) days + 1;
            }

            // 미션
            missionList = couple.getQuestions();   // 존재하는 경우만

            // 채팅방 (없으면 null)
            roomId = chatRoomRepository.findByCouple(couple)
                    .map(ChatRoom::getId)
                    .orElse(null);
        }

        return new HomeDto(
                anniversary,
                partnerName,   // 커플 없으면 null
                missionList,   // 커플 없으면 null
                dDay,          // 커플 없으면 null
                roomId         // 커플 없으면 null
        );
    }

}
