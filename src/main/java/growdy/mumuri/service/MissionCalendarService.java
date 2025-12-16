package growdy.mumuri.service;
import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.*;
import growdy.mumuri.dto.MissionDaySummaryDto;
import growdy.mumuri.dto.MissionDetailDto;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.MissionRepository;
import growdy.mumuri.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissionCalendarService {

    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final PhotoRepository photoRepository;
    private final S3Upload s3Upload;
    private final MissionRepository missionRepository;

    /**
     * 🗓 월 단위 미션 캘린더 (썸네일용)
     */
    @Transactional(readOnly = true)
    public List<MissionDetailDto> getMonthly(Long memberId, int year, int month) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElseThrow(() -> new IllegalStateException("커플이 아닙니다."));

        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = last.atTime(LocalTime.MAX);

        List<Photo> photos = photoRepository
                .findByCoupleIdAndDeletedFalseAndCreatedAtBetween(couple.getId(), from, to);

        Long myId = me.getId();

        // 업로더(Member) 정보 필요하므로 한 번에 로딩
        Map<Long, Member> memberCache = loadMembersForPhotos(photos);

        return photos.stream()
                .map(p -> {
                    Long uploaderId = p.getUploadedBy();
                    Member uploader = memberCache.get(uploaderId);

                    MissionOwnerType type =
                            Objects.equals(uploaderId, myId) ? MissionOwnerType.ME : MissionOwnerType.PARTNER;

                    String nickname = uploader != null ? uploader.getName() : "알 수 없음";

                    String texts = null;
                    Long missionId= p.getMissionId();
                    Mission miss= missionRepository.findById(missionId).orElse(null);
                    texts = miss != null ? miss.getTitle() : null;
                    String url = s3Upload.presignedGetUrl(p.getS3Key(), Duration.ofMinutes(10));

                    String missionText = p.getDescription(); // 또는 미션 연동되면 미션 타이틀로 교체

                    return new MissionDetailDto(
                            p.getId(),
                            type,
                            nickname,
                            p.getCreatedAt(),
                            url,
                            texts
                    );
                })
                .sorted(Comparator.comparing(MissionDetailDto::createdAt))
                .toList();
    }

    /**
     * 📸 특정 날짜 미션 상세 (나/애인 구분)
     */
    @Transactional(readOnly = true)
    public List<MissionDetailDto> getDaily(Long memberId, LocalDate date) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElseThrow(() -> new IllegalStateException("커플이 아닙니다."));

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        List<Photo> photos = photoRepository
                .findByCoupleIdAndDeletedFalseAndCreatedAtBetween(couple.getId(), from, to);

        Long myId = me.getId();
        Long partnerId = getPartnerId(couple, myId);

        // 업로더(Member) 정보 필요하므로 한 번에 로딩
        Map<Long, Member> memberCache = loadMembersForPhotos(photos);

        return photos.stream()
                .map(p -> {
                    Long uploaderId = p.getUploadedBy();
                    Member uploader = memberCache.get(uploaderId);

                    MissionOwnerType type =
                            Objects.equals(uploaderId, myId) ? MissionOwnerType.ME : MissionOwnerType.PARTNER;

                    String nickname = uploader != null ? uploader.getName() : "알 수 없음";


                    String texts = null;
                    Long missionId= p.getMissionId();
                    Mission miss= missionRepository.findById(missionId).orElse(null);
                    texts = miss != null ? miss.getTitle() : null;
                    String url = s3Upload.presignedGetUrl(
                            p.getS3Key(),
                            Duration.ofMinutes(10)
                    );

                    String missionText = p.getDescription(); // 질문/미션 문구

                    return new MissionDetailDto(
                            p.getId(),
                            type,
                            nickname,
                            p.getCreatedAt(),
                            url,
                            texts
                    );
                })
                .sorted(Comparator.comparing(MissionDetailDto::createdAt))
                .toList();
    }

    // ===== helper =====

    private Long getPartnerId(Couple couple, Long myId) {
        if (couple.getMember1() != null && couple.getMember1().getId().equals(myId)) {
            return couple.getMember2() != null ? couple.getMember2().getId() : null;
        }
        if (couple.getMember2() != null && couple.getMember2().getId().equals(myId)) {
            return couple.getMember1() != null ? couple.getMember1().getId() : null;
        }
        return null;
    }

    private Map<Long, Member> loadMembersForPhotos(List<Photo> photos) {
        Set<Long> ids = photos.stream()
                .map(Photo::getUploadedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Collections.emptyMap();

        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
    }
}