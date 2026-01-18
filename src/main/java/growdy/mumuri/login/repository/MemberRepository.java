package growdy.mumuri.login.repository;

import growdy.mumuri.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface MemberRepository extends JpaRepository<Member, Long> {
        Optional<Member> findById(Long Id);
        Optional<Member> findByCoupleCodeAndStatusAndIdNot(String coupleCode, String status, Long excludeId);
        boolean existsByCoupleCode(String coupleCode);
        Optional<Member> findByKakaoId(Long kakaoId);
        Optional<Member> findByAppleId(String appleId);
    }

