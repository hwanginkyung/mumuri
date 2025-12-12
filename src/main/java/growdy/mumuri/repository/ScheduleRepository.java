package growdy.mumuri.repository;

import growdy.mumuri.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByOwnerIdAndStartAtBetween(Long ownerId,
                                                  LocalDateTime start,
                                                  LocalDateTime end);

    List<Schedule> findByCoupleIdAndStartAtBetween(Long coupleId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);
}
