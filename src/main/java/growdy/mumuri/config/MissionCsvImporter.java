package growdy.mumuri.config;

import growdy.mumuri.domain.Mission;
import growdy.mumuri.domain.MissionDifficulty;
import growdy.mumuri.repository.MissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MissionCsvImporter {

    private final MissionRepository missionRepository;

    @PostConstruct
    public void importMissionsFromCsv() throws Exception {
        // ✅ 이미 데이터가 있으면 다시 실행 안 함
        if (missionRepository.count() > 0) {
            System.out.println("✅ Missions already exist, skipping import.");
            return;
        }

        Path csvPath = Path.of("/home/ubuntu/missions.csv");
        System.out.println("🚀 Importing missions from: " + csvPath);

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath.toFile()))) {
            br.readLine(); // header skip
            String line;
            List<Mission> missions = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 3) continue;

                String title = cols[0].trim();
                String category = cols[1].trim();
                String difficultyStr = cols[2].trim().toUpperCase();

                MissionDifficulty difficulty;
                try {
                    difficulty = MissionDifficulty.valueOf(difficultyStr);
                } catch (Exception e) {
                    difficulty = MissionDifficulty.NORMAL;
                }

                Mission m = new Mission();
                m.setTitle(title);
                m.setCategory(category);
                m.setDifficulty(difficulty);
                m.setActive(true);
                missions.add(m);
            }

            missionRepository.saveAll(missions);
            System.out.println("✅ Imported " + missions.size() + " missions successfully!");
        }
    }
}
