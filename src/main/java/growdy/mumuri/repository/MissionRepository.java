package growdy.mumuri.repository;

import growdy.mumuri.domain.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByActiveTrue();
}
