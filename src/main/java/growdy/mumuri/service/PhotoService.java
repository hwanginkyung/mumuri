package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.MissionOwnerType;
import growdy.mumuri.domain.Photo;
import growdy.mumuri.dto.MissionDetailDto;
import growdy.mumuri.dto.PhotoResponseDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {
    private final CoupleRepository coupleRepository;
    private final PhotoRepository photoRepository;
    private final S3Upload s3Upload;
    private final MemberRepository memberRepository;

    public String uploadPhoto(Long coupleId, MultipartFile file, Long userId,Long missionId) {
        String key = null;
        Couple couple = null;
        String s3Url= null;
        String urls=null;
        Long missionsId=null;
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
                .missionId(missionsId)
                .build();
        photoRepository.save(photo);
        urls= s3Upload.presignedGetUrl(photo.getS3Key(),Duration.ofMinutes(10));
        return key;
    }
    /** 사진 한장  (presigned URL 반환) */
    @Transactional(readOnly = true)
    public PhotoResponseDto getOne(Long coupleId, Long photoId) {
        Photo p = photoRepository.findByIdAndCoupleId(photoId, coupleId);

        if (p.isDeleted()) {
            throw new IllegalStateException("삭제된 사진입니다.");
        }

        String url = s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(10));
        return new PhotoResponseDto(p.getId(), url, p.getUploadedBy(),p.getCreatedAt());
    }

    /** 커플 앨범 목록 (presigned URL 포함) */
    @Transactional(readOnly = true)
    public List<PhotoResponseDto> listByCouple(Long coupleId) {
        return photoRepository.findByCoupleIdAndDeletedFalseOrderByIdDesc(coupleId).stream()
                .map(p -> new PhotoResponseDto(
                        p.getId(),
                        s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(10)),
                        p.getUploadedBy(),
                        p.getCreatedAt()
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

    @Transactional(readOnly = true)
    public Page<MissionDetailDto> getGallery(Long memberId, int page) {

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElseThrow(() -> new IllegalStateException("커플이 아닙니다."));

        Pageable pageable = PageRequest.of(
                page,
                51,
                Sort.by(Sort.Direction.DESC, "createdAt", "id") // 최신순 안정정렬
        );

        Page<Photo> photosPage = photoRepository.findByCoupleIdAndDeletedFalse(couple.getId(), pageable);

        // 업로더 닉네임 조회를 N+1 안나게 한 번에 캐시
        Set<Long> uploaderIds = photosPage.getContent().stream()
                .map(Photo::getUploadedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Member> memberCache = memberRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return photosPage.map(p -> {
            Long uploaderId = p.getUploadedBy();
            Member uploader = memberCache.get(uploaderId);

            MissionOwnerType ownerType =
                    Objects.equals(uploaderId, memberId) ? MissionOwnerType.ME : MissionOwnerType.PARTNER;

            String nickname = uploader != null ? uploader.getName() : "알 수 없음";

            String url = s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(30));

            return new MissionDetailDto(
                    p.getId(),
                    ownerType,
                    nickname,
                    p.getCreatedAt(),
                    url,
                    p.getDescription()
            );
        });
    }

}

