package growdy.mumuri.controller;

import growdy.mumuri.domain.*;
import growdy.mumuri.dto.MainDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.repository.CoupleMissionProgressRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import growdy.mumuri.service.DdayService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final CoupleRepository coupleRepository;
    private final CoupleMissionRepository coupleMissionRepository;
    private final CoupleMissionProgressRepository coupleMissionProgressRepository;
    private final MissionRepository missionRepository;
    private final DdayService dayService;

    @Transactional
    @GetMapping("/user/main")
    public MainDto mainDto(@AuthenticationPrincipal CustomUserDetails user) {

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(user.getId(), user.getId())
                .orElseThrow();

        long dday = dayService.getDday(couple.getId());
        LocalDate today = LocalDate.now();

        // Mission까지 fetch해서 Lazy 문제 방지
        List<CoupleMission> missions = coupleMissionRepository
                .findByCoupleIdAndMissionDateWithMission(couple.getId(), today);

        if (missions.isEmpty()) {
            List<Mission> allMissions = missionRepository.findByActiveTrue();
            Collections.shuffle(allMissions);
            List<Mission> selected = allMissions.stream().limit(6).toList();

            Long member1Id = couple.getMember1().getId();
            Long member2Id = couple.getMember2().getId();

            for (Mission mission : selected) {
                // CoupleMission 생성 후 저장
                CoupleMission cm = new CoupleMission(couple, mission, today);
                cm = coupleMissionRepository.save(cm); // 영속화

                // 진행 상태 생성 후 양방향 세팅
                CoupleMissionProgress p1 = new CoupleMissionProgress(cm, member1Id);
                CoupleMissionProgress p2 = new CoupleMissionProgress(cm, member2Id);

                cm.addProgress(p1);
                cm.addProgress(p2);

                coupleMissionProgressRepository.saveAll(List.of(p1, p2));
            }

            // 다시 조회
            missions = coupleMissionRepository.findByCoupleIdAndMissionDateWithMission(couple.getId(), today);
        }

        // 미션 제목 리스트화
        List<String> missionTitles = missions.stream()
                .map(cm -> cm.getMission().getTitle())
                .toList();

        // DTO 생성
        MainDto mainDto = new MainDto();
        mainDto.setDday(dday);
        mainDto.setMissionLists(missionTitles);

        return mainDto;
    }
}
