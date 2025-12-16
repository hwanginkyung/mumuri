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

        List<Photo> photos = photoRepository
                .findByCoupleIdAndDeletedFalseAndCreatedAtBetween(
                        couple.getId(),
                        first.atStartOfDay(),
                        last.atTime(LocalTime.MAX)
                );

        Long myId = me.getId();

        Map<Long, Member> memberCache = loadMembersForPhotos(photos);
        Map<Long, Mission> missionCache = loadMissionsForPhotos(photos);

        return photos.stream()
                .map(p -> {
                    Long uploaderId = p.getUploadedBy();
                    Member uploader = memberCache.get(uploaderId);

                    MissionOwnerType type =
                            Objects.equals(uploaderId, myId)
                                    ? MissionOwnerType.ME
                                    : MissionOwnerType.PARTNER;

                    String nickname = uploader != null ? uploader.getName() : "알 수 없음";

                    Mission mission = missionCache.get(p.getMissionId());
                    String missionTitle = mission != null ? mission.getTitle() : null;

                    String url = s3Upload.presignedGetUrl(
                            p.getS3Key(),
                            Duration.ofMinutes(10)
                    );

                    return new MissionDetailDto(
                            p.getId(),
                            type,
                            nickname,
                            p.getCreatedAt(),
                            url,
                            missionTitle
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

        List<Photo> photos = photoRepository
                .findByCoupleIdAndDeletedFalseAndCreatedAtBetween(
                        couple.getId(),
                        date.atStartOfDay(),
                        date.atTime(LocalTime.MAX)
                );

        Long myId = me.getId();

        Map<Long, Member> memberCache = loadMembersForPhotos(photos);
        Map<Long, Mission> missionCache = loadMissionsForPhotos(photos);

        return photos.stream()
                .map(p -> {
                    Long uploaderId = p.getUploadedBy();
                    Member uploader = memberCache.get(uploaderId);

                    MissionOwnerType type =
                            Objects.equals(uploaderId, myId)
                                    ? MissionOwnerType.ME
                                    : MissionOwnerType.PARTNER;

                    String nickname = uploader != null ? uploader.getName() : "알 수 없음";

                    Mission mission = missionCache.get(p.getMissionId());
                    String missionTitle = mission != null ? mission.getTitle() : null;

                    String url = s3Upload.presignedGetUrl(
                            p.getS3Key(),
                            Duration.ofMinutes(10)
                    );

                    return new MissionDetailDto(
                            p.getId(),
                            type,
                            nickname,
                            p.getCreatedAt(),
                            url,
                            missionTitle
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
    private Map<Long, Mission> loadMissionsForPhotos(List<Photo> photos) {
        Set<Long> missionIds = photos.stream()
                .map(Photo::getMissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (missionIds.isEmpty()) return Collections.emptyMap();

        return missionRepository.findAllById(missionIds).stream()
                .collect(Collectors.toMap(Mission::getId, m -> m));
    }
}