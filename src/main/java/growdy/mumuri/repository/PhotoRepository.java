package growdy.mumuri.repository;

import growdy.mumuri.domain.CouplePhoto;
import growdy.mumuri.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByCoupleIdAndDeletedFalseOrderByIdDesc(Long coupleId);
    Photo findByIdAndCoupleId(Long id, Long coupleId);
}
