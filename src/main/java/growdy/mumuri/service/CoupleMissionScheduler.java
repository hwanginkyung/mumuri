package growdy.mumuri.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.Mission;
import growdy.mumuri.domain.MissionSchedule;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import growdy.mumuri.repository.MissionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        LocalDate today = LocalDate.now();
        List<Mission> all = missionRepository.findByActiveTrue();
        List<Couple> couples = coupleRepository.findAll();

        for (Couple couple : couples) {
            Set<Long> completed = coupleMissionRepository.findCompletedMissionIds(couple.getId());
            List<Mission> available = all.stream()
                    .filter(m -> !completed.contains(m.getId()))
                    .toList();

            if (available.isEmpty()) {
                // 만약 모든 미션을 다 했다면 전체 목록 리셋
                available = all;
            }

            Collections.shuffle(available);
            available.stream().limit(6).forEach(m -> {
                CoupleMission cm = new CoupleMission(couple, m, today);
                coupleMissionRepository.save(cm);
            });
        }
    }
}
