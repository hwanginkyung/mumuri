package growdy.mumuri.service;


import growdy.mumuri.aws.S3Service;
import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.CouplePhoto;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.Photo;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CouplePhotoRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final CouplePhotoRepository couplePhotoRepository;
    private final PhotoRepository photoRepository;
    private final S3Service s3Service;
    private final S3Upload s3Upload;

    @Transactional
    public void checkAndSetCouple(Long userId, String coupleCode) {
        Member coupleMember = memberRepository
                .findByCoupleCodeAndStatusAndIdNot(coupleCode, "solo", userId)
                .orElseThrow(() -> new IllegalArgumentException("No matching couple found"));

        Member user = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Couple 생성
        Couple couple = new Couple();
        couple.setMember1(user);
        couple.setMember2(coupleMember);
        couple.setCoupleCode(coupleCode);
        coupleRepository.save(couple);

        // 상태 변경
        user.setStatus("couple");
        coupleMember.setStatus("couple");
        user.setCoupleCode(coupleCode);
        coupleMember.setCoupleCode(coupleCode);
    }
    @Transactional
    public Couple test(Member user){
        // Couple 생성
        Member member = new Member();
        member.setId(user.getId());
        Couple couple = new Couple();
        couple.setMember1(member);
        couple.setMember2(user);
        coupleRepository.save(couple);
        return couple;
    }

}
