package growdy.mumuri.service;

import growdy.mumuri.domain.*;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class CoupleMissionScheduler {

    private final CoupleRepository coupleRepository;
    private final MissionRepository missionRepository;
    private final CoupleMissionRepository coupleMissionRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void assignDailyCoupleMissions() {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Mission> all = missionRepository.findByActiveTrue();
        List<Couple> couples = coupleRepository.findAll();

        for (Couple couple : couples) {
            Long member1Id = couple.getMember1().getId();
            Long member2Id = couple.getMember2().getId();

            // 커플이 완료한 미션 목록
            Set<Long> completed = coupleMissionRepository.findCompletedMissionIds(couple.getId());

            // 아직 안 한 미션만 필터링
            List<Mission> available = all.stream()
                    .filter(m -> !completed.contains(m.getId()))
                    .toList();

            // 다 했으면 전체로 리셋
            if (available.isEmpty()) available = all;

            // 무작위 6개 뽑기
            Collections.shuffle(available);

            available.stream().limit(6).forEach(mission -> {
                CoupleMission cm = new CoupleMission(couple, mission, today);

                // progress 두 개 자동 생성
                new CoupleMissionProgress(cm, member1Id);
                new CoupleMissionProgress(cm, member2Id);

                coupleMissionRepository.save(cm);  // cascade로 progress 자동 저장됨
            });
        }
    }
}
