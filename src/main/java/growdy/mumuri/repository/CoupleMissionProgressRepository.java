package growdy.mumuri.repository;

import growdy.mumuri.domain.CoupleMissionProgress;
import growdy.mumuri.domain.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoupleMissionProgressRepository extends JpaRepository<CoupleMissionProgress, Long> {
    Optional<CoupleMissionProgress> findByCoupleMissionIdAndUserId(Long coupleMissionId, Long userId);
    long countByCoupleMissionIdAndStatus(Long coupleMissionId, ProgressStatus status);
    void deleteByCoupleMissionIdIn(List<Long> coupleMissionIds);
}
