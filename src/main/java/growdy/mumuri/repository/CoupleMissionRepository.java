package growdy.mumuri.repository;

import growdy.mumuri.domain.CoupleMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface CoupleMissionRepository extends JpaRepository<CoupleMission, Long> {
    @Query("""
        SELECT DISTINCT cm
        FROM CoupleMission cm
        LEFT JOIN FETCH cm.mission m
        LEFT JOIN FETCH cm.progresses p
        WHERE cm.couple.id = :coupleId
          AND cm.missionDate = :date
    """)
    List<CoupleMission> findTodayWithProgresses(
            @Param("coupleId") Long coupleId,
            @Param("date") LocalDate date
    );

    @Query("""
        select distinct cm.mission.id
        from CoupleMission cm
        where cm.couple.id = :coupleId
          and cm.status = 'COMPLETED'
    """)
    Set<Long> findCompletedMissionIds(@Param("coupleId") Long coupleId);

    List<CoupleMission> findByCoupleIdAndMissionDate(Long coupleId, LocalDateTime missionDate);
}