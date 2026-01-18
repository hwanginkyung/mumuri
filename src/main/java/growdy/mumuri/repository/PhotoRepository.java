package growdy.mumuri.repository;

import growdy.mumuri.domain.CouplePhoto;
import growdy.mumuri.domain.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface PhotoRepository extends JpaRepository<Photo, Long> {
    void deleteByCoupleId(Long coupleId);
    List<Photo> findByCoupleIdAndDeletedFalseOrderByIdDesc(Long coupleId);
    Photo findByIdAndCoupleId(Long id, Long coupleId);
    List<Photo> findByCoupleIdAndDeletedFalseAndCreatedAtBetween(
            Long coupleId,
            LocalDateTime from,
            LocalDateTime to
    );
    Page<Photo> findByCoupleIdAndDeletedFalse(Long coupleId, Pageable pageable);
    Optional<Photo> findFirstByCoupleIdAndDeletedFalseOrderByCreatedAtDesc(Long coupleId);
}
