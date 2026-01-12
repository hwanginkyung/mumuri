package growdy.mumuri.repository;

import growdy.mumuri.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByOwnerIdAndCoupleIsNullAndStartAtBetween(Long ownerId,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end);

    List<Schedule> findByCoupleIdAndStartAtBetween(Long coupleId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Schedule s where s.couple.id = :coupleId")
    int deleteByCoupleId(@Param("coupleId") Long coupleId);
}
