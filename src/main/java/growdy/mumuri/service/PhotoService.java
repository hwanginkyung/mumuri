package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Photo;
import growdy.mumuri.dto.PhotoResponseDto;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final CoupleRepository coupleRepository;
    private final PhotoRepository photoRepository;
    private final S3Upload s3Upload;
    public String uploadPhoto(Long coupleId, MultipartFile file, Long userId,Long missionId) {
        String key = null;
        Couple couple = null;
        String s3Url= null;
        try {
            key = "couples/" + coupleId + "/"+missionId+"/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            couple = coupleRepository.findById(coupleId).orElse(null);
            // S3 업로드 + S3 URL 생성
            s3Url = s3Upload.upload(file,key,"image/jpeg");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // DB 저장
        Photo photo = Photo.builder()
                .couple(couple)
                .s3Key(key)
                .url(s3Url)
                .uploadedBy(userId)
                .build();
        photoRepository.save(photo);
        return s3Url;
    }
    /** 사진 한장  (presigned URL 반환) */
    @Transactional(readOnly = true)
    public PhotoResponseDto getOne(Long coupleId, Long photoId) {
        Photo p = photoRepository.findByIdAndCoupleId(photoId, coupleId);

        if (p.isDeleted()) {
            throw new IllegalStateException("삭제된 사진입니다.");
        }

        String url = s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(10));
        return new PhotoResponseDto(p.getId(), url, p.getUploadedBy());
    }

    /** 커플 앨범 목록 (presigned URL 포함) */
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> listByCouple(Long coupleId) {
        return photoRepository.findByCoupleIdAndDeletedFalseOrderByIdDesc(coupleId).stream()
                .map(p -> new PhotoResponseDto(
                        p.getId(),
                        s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(10)),
                        p.getUploadedBy()
                ))
                .toList();
    }

    /** 삭제: soft delete + (선택) S3 물리 삭제 */
    @Transactional
    public void delete(Long photoId,Long coupleId, boolean hardDelete) {
        Photo p = photoRepository.findByIdAndCoupleId(photoId, coupleId);
        if (p.isDeleted()) return; // 이미 삭제됨
        p.softDelete(); // soft delete
        if (hardDelete) {
            // 정말 S3 객체까지 지울 때만 사용 (복구 불가)
            s3Upload.deleteObject(p.getS3Key());
        }
    }
}

