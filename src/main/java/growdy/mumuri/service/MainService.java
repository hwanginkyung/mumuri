package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.*;
import growdy.mumuri.dto.HomeDto;
import growdy.mumuri.dto.MissionSummaryDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.ChatRoomRepository;
import growdy.mumuri.repository.CoupleMissionRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MainService {
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;
    private final CoupleMissionRepository coupleMissionRepository;
    private final PhotoRepository photoRepository;
    private final S3Upload s3Upload;

    @Transactional(readOnly = true)
    public HomeDto getHome(CustomUserDetails user) {
        Member me = memberRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Optional<Couple> optionalCouple =
                coupleRepository.findByMember1IdOrMember2Id(me.getId(), me.getId());

        LocalDate anniversary = me.getAnniversary();
        Integer dDay = null;
        Long coupleId = null;
        Long roomId = null;
        Integer missionCompletedCount = null;
        HomeDto.MainPhotoDto mainPhotoDto = null;

        // ✅ 프로필 URL (내꺼는 커플 없어도 내려줄 수 있음)
        String myProfileImageUrl = null;
        if (me.getProfileImageKey() != null) {
            myProfileImageUrl = s3Upload.presignedGetUrl(me.getProfileImageKey(), Duration.ofMinutes(30));
        }

        String partnerProfileImageUrl = null;

        if (optionalCouple.isPresent()) {
            Couple couple = optionalCouple.get();
            coupleId = couple.getId();

            Member partner = couple.getMember1().getId().equals(me.getId())
                    ? couple.getMember2()
                    : couple.getMember1();

            if (partner != null && partner.getProfileImageKey() != null) {
                partnerProfileImageUrl = s3Upload.presignedGetUrl(partner.getProfileImageKey(), Duration.ofMinutes(30));
            }

            if (anniversary != null) {
                long days = ChronoUnit.DAYS.between(anniversary, LocalDate.now());
                dDay = (int) days + 1;
            }

            roomId = chatRoomRepository.findByCouple(couple)
                    .map(ChatRoom::getId)
                    .orElse(null);

            missionCompletedCount = coupleMissionRepository
                    .countByCoupleIdAndStatus(coupleId, MissionStatus.COMPLETED);

            Photo mainPhoto = me.getMainPhoto();

            if (mainPhoto != null && !mainPhoto.isDeleted()) {
                Long uploaderId = mainPhoto.getUploadedBy();

                MissionOwnerType uploaderType =
                        (uploaderId != null && uploaderId.equals(me.getId()))
                                ? MissionOwnerType.ME
                                : MissionOwnerType.PARTNER;

                String uploaderNickname = "알 수 없음";
                if (uploaderId != null) {
                    uploaderNickname = memberRepository.findById(uploaderId)
                            .map(Member::getName)
                            .orElse("알 수 없음");
                }

                String url = s3Upload.presignedGetUrl(mainPhoto.getS3Key(), Duration.ofMinutes(30));

                mainPhotoDto = new HomeDto.MainPhotoDto(
                        mainPhoto.getId(),
                        url,
                        uploaderType,
                        uploaderNickname,
                        mainPhoto.getCreatedAt()
                );
            }
            else{
                mainPhotoDto = new HomeDto.MainPhotoDto(
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }
        }

        return new HomeDto(
                anniversary,
                dDay,
                coupleId,
                roomId,
                missionCompletedCount,
                mainPhotoDto,
                myProfileImageUrl,
                partnerProfileImageUrl
        );
    }


}
