package growdy.mumuri.controller;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.Mission;
import growdy.mumuri.dto.MainDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import growdy.mumuri.service.DdayService;
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
    private final DdayService   dayService;
    private final CoupleRepository coupleRepository;
    private final CoupleMissionRepository coupleMissionRepository;
    private final MissionRepository missionRepository;

    @GetMapping("/user/main")
    public MainDto mainDto(@AuthenticationPrincipal CustomUserDetails user){
        Couple couple= coupleRepository.findByMember1IdOrMember2Id(user.getId(),user.getId()).orElseThrow();
        long dday=dayService.getDday(couple.getId());
        LocalDate today = LocalDate.now();
        List<CoupleMission> missions = coupleMissionRepository.findByCoupleIdAndMissionDate(couple.getId(), today);
        if (missions.isEmpty()) {
            List<Mission> allMissions = missionRepository.findByActiveTrue();
            Collections.shuffle(allMissions);

            List<Mission> selected = allMissions.stream().limit(6).toList();
            for (Mission mission : selected) {
                CoupleMission cm = new CoupleMission(couple, mission, today);
                coupleMissionRepository.save(cm);
            }

            missions = coupleMissionRepository.findByCoupleIdAndMissionDate(couple.getId(), today);
        }

        // 4️⃣ D-Day 계산

        // 5️⃣ 미션 이름만 추출
        List<String> missionTitles = missions.stream()
                .map(cm -> cm.getMission().getTitle())
                .toList();

        // 6️⃣ DTO 구성
        MainDto mainDto = new MainDto();
        mainDto.setDday(dday);
        mainDto.setMissionLists(missionTitles);

        return mainDto;
    }
}
