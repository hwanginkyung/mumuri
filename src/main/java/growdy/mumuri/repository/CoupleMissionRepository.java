package growdy.mumuri.repository;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.domain.MissionStatus;
import growdy.mumuri.dto.MissionSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface CoupleMissionRepository extends JpaRepository<CoupleMission, Long> {
    int countByCoupleIdAndStatus(Long coupleId, MissionStatus status);
    long countByCoupleIdAndMissionDate(Long coupleId, LocalDate missionDate);


    void deleteByCoupleId(Long coupleId);
    @Query("select cm.id from CoupleMission cm where cm.couple.id = :coupleId")
    List<Long> findIdsByCoupleId(@Param("coupleId") Long coupleId);
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


    List<CoupleMission> findByCoupleIdAndStatusIn(Long coupleId, List<MissionStatus> statuses);

    @Query("SELECT cm FROM CoupleMission cm JOIN FETCH cm.mission " +
            "WHERE cm.couple.id = :coupleId AND cm.missionDate = :date")
    List<CoupleMission> findByCoupleIdAndMissionDateWithMission(@Param("coupleId") Long coupleId,
                                                                @Param("date") LocalDate date);

    List<CoupleMission> findByCoupleIdAndMissionDate(Long coupleId, LocalDate date);

    @Query("""
select new growdy.mumuri.dto.MissionSummaryDto(
    cm.id,
    m.title,
    cm.status
)
from CoupleMission cm
join cm.mission m
where cm.couple.id = :coupleId
""")
    List<MissionSummaryDto> findMissionSummaries(Long coupleId);
    @Query("""
    select cm
    from CoupleMission cm
    join fetch cm.mission m
    where cm.couple.id = :coupleId
      and cm.missionDate between :start and :end
""")
    List<CoupleMission> findWithMissionBetween(
            @Param("coupleId") Long coupleId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    select cm
    from CoupleMission cm
    join fetch cm.mission m
    where cm.couple.id = :coupleId
      and cm.missionDate in :dates
""")
    List<CoupleMission> findWithMissionByDates(
            @Param("coupleId") Long coupleId,
            @Param("dates") List<LocalDate> dates
    );
}