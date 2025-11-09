package growdy.mumuri.config;

import growdy.mumuri.domain.Mission;
import growdy.mumuri.domain.MissionDifficulty;
import growdy.mumuri.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionCsvImporter {

    private final MissionRepository missionRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void importMissionsFromCsv() {
        Path csvPath = Path.of("/home/ubuntu/missions.csv");
        log.info("🚀 Importing missions from: {}", csvPath);

        if (missionRepository.count() > 0) {
            log.info("✅ Missions already exist, skipping import.");
            return;
        }

        List<Mission> missions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath.toFile()))) {
            br.readLine(); // header skip
            String line;
            int lineCount = 0;

            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] cols = line.split(",");

                if (cols.length < 3) {
                    log.warn("⚠️ Skipping invalid line #{}: {}", lineCount, line);
                    continue;
                }

                String title = cols[0].trim();
                String category = cols[1].trim();
                String difficultyStr = cols[2].trim().toUpperCase();

                MissionDifficulty difficulty;
                try {
                    difficulty = MissionDifficulty.valueOf(difficultyStr);
                } catch (Exception e) {
                    log.warn("⚠️ Unknown difficulty '{}' in line #{} -> default NORMAL", difficultyStr, lineCount);
                    difficulty = MissionDifficulty.NORMAL;
                }

                Mission mission = new Mission();
                mission.setTitle(title);
                mission.setCategory(category);
                mission.setDifficulty(difficulty);
                mission.setActive(true);
                missions.add(mission);
            }

            missionRepository.saveAll(missions);
            log.info("✅ Imported {} missions successfully!", missions.size());

        } catch (Exception e) {
            log.error("❌ Failed to import missions from CSV: {}", e.getMessage(), e);
        }
    }
}
