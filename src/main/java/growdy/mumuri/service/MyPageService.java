package growdy.mumuri.service;

import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.HomeDto;
import growdy.mumuri.dto.MyPageDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    public MyPageDto mypage(CustomUserDetails user) {

        // 1. 유저 조회
        Member me = memberRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // 2. 커플 optional
        Optional<Couple> optionalCouple =
                coupleRepository.findByMember1IdOrMember2Id(me.getId(), me.getId());

        // 기본값들
        LocalDate anniversary = me.getAnniversary();
        Integer dDay = null;
        LocalDate partnerBirthday = null;

        if (optionalCouple.isPresent()) {
            Couple couple = optionalCouple.get();

            Member partner =
                    couple.getMember1().getId().equals(me.getId()) ?
                            couple.getMember2() : couple.getMember1();

            if (partner != null) {
                partnerBirthday = partner.getBirthday();
            }

            // D+ 계산
            if (anniversary != null) {
                long days = ChronoUnit.DAYS.between(anniversary, LocalDate.now());
                dDay = (int) days + 1;
            }
        }

        return new MyPageDto(
                me.getName(),
                me.getBirthday(),
                anniversary,
                partnerBirthday, // 커플 없으면 null
                dDay              // 커플 없으면 null
        );
    }
}
