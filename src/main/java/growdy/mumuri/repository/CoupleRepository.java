package growdy.mumuri.repository;

import growdy.mumuri.domain.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
    Optional<Couple> findByMember1IdOrMember2Id(Long member1Id, Long member2Id);
}
