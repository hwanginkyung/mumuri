package growdy.mumuri.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DdayService {

    private final CoupleRepository coupleRepository;

    public long getDday(Long coupleId) {
        Couple couple = coupleRepository.findById(coupleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 커플"));
        return ChronoUnit.DAYS.between(couple.getAnniversary(), LocalDate.now());
    }
}