package growdy.mumuri.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.CoupleMissionProgress;
import growdy.mumuri.domain.Mission;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoupleMissionScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAILY_MISSION_COUNT = 6;

    private final CoupleRepository coupleRepository;
    private final MissionRepository missionRepository;
    private final CoupleMissionRepository coupleMissionRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void assignDailyCoupleMissions() {
        LocalDate today = LocalDate.now(KST);

        log.info("[SCHED] assignDailyCoupleMissions fired. today={}", today);

        List<Mission> all = missionRepository.findByActiveTrue();
        if (all.isEmpty()) {
            log.warn("[SCHED] No active missions. Skip. today={}", today);
            return;
        }

        List<Couple> couples = coupleRepository.findAll();
        if (couples.isEmpty()) {
            log.info("[SCHED] No couples. Skip. today={}", today);
            return;
        }

        for (Couple couple : couples) {
            // ✅ (중요) 재시작/중복 실행 방어: 오늘 이미 있으면 스킵
            long existingCount = coupleMissionRepository.countByCoupleIdAndMissionDate(couple.getId(), today);
            if (existingCount > 0) {
                log.info("[SCHED] Skip coupleId={} because missions already exist. count={}, today={}",
                        couple.getId(), existingCount, today);
                continue;
            }

            Long member1Id = couple.getMember1().getId();
            Long member2Id = couple.getMember2().getId();

            // 커플이 완료한 미션 목록
            Set<Long> completed = coupleMissionRepository.findCompletedMissionIds(couple.getId());

            // 아직 안 한 미션만 필터링 (✅ shuffle 가능하도록 가변 리스트로)
            List<Mission> available = new ArrayList<>(
                    all.stream()
                            .filter(m -> !completed.contains(m.getId()))
                            .toList()
            );

            // 다 했으면 전체로 리셋
            if (available.isEmpty()) {
                available = new ArrayList<>(all);
            }

            Collections.shuffle(available);

            List<Mission> picked = available.stream().limit(DAILY_MISSION_COUNT).toList();
            for (Mission mission : picked) {
                CoupleMission cm = new CoupleMission(couple, mission, today);

                // progress 두 개 자동 생성 (cm에 addProgress 내부에서 양방향 세팅되게 돼있으면 베스트)
                new CoupleMissionProgress(cm, member1Id);
                new CoupleMissionProgress(cm, member2Id);

                coupleMissionRepository.save(cm); // cascade로 progress 저장
            }

            log.info("[SCHED] Created {} missions for coupleId={}, today={}",
                    picked.size(), couple.getId(), today);
        }
    }
}
