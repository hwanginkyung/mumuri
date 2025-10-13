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
    public void uploadPhoto(Long memId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        Couple couple = coupleRepository.findByMember1IdOrMember2Id(memId,memId)
                .orElseThrow(() -> new IllegalArgumentException("Couple not found"));;
        Long coupleId= couple.getId();
        Member member = memberRepository.findById(memId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        // 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        // ContentType
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 업로드 경로 (날짜별 관리)
        String path = String.format("uploads/couple/%d/photos/%s/%s",
                coupleId,
                LocalDate.now(),
                filename);
        String path2 =String.format("uploads/member/%d/photos/%s/%s",
                memId,
                LocalDate.now(),
                filename);
        String s3Url = s3Upload.upload(file, path, contentType);
        String s3Url2 = s3Upload.upload(file, path2, contentType);

        CouplePhoto photo = new CouplePhoto();
        photo.setPhotoUrl(s3Url);
        photo.setCouple(couple);
        Photo photo2 = new Photo();
        photo2.setPhotoUrl(s3Url2);
        photo2.setMember(member);
        couplePhotoRepository.save(photo);
        photoRepository.save(photo2);
    }

}
