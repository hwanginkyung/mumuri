package growdy.mumuri.repository;

import growdy.mumuri.domain.MissionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MissionScheduleRepository extends JpaRepository<MissionSchedule, Long> {
    boolean existsByMissionDate(LocalDate date);
    List<MissionSchedule> findByMissionDate(LocalDate date);
}