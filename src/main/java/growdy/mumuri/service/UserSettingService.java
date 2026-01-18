package growdy.mumuri.service;

import growdy.mumuri.aws.S3Upload;
import growdy.mumuri.domain.Member;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.login.service.MemberService;
import growdy.mumuri.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final CoupleRepository coupleRepository;
    private final S3Upload s3Upload;
    @Transactional
    public void updateMemberName(Long memberId, String name) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setName(name);
    }
    @Transactional
    public void updateMemberBirthday(Long memberId, LocalDate birthday) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setBirthday(birthday);
    }
    @Transactional
    public void updateMemberAnniversary(Long memberId, LocalDate anniversary) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        coupleRepository.findByMember1IdOrMember2Id(memberId, memberId)
                .ifPresentOrElse(couple -> {

                    couple.setAnniversary(anniversary);
                    Member m1 = couple.getMember1();
                    Member m2 = couple.getMember2();
                    if (m1 != null) m1.setAnniversary(anniversary);
                    if (m2 != null) m2.setAnniversary(anniversary);

                }, () -> {
                    me.setAnniversary(anniversary);
                });
    }

    @Transactional
    public String updateProfilePhoto(Long memberId, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // (선택) 기존 프로필 사진 S3 삭제하고 싶으면 oldKey 저장해두고 삭제
        String oldKey = member.getProfileImageKey();

        String key = "members/" + memberId + "/profile/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 업로드 (너 프로젝트 s3Upload 방식에 맞춰 호출만 바꿔주면 됨)
        s3Upload.upload(file, key,"image/jpeg"); // <- 네 업로드 메서드에 맞게

        member.setProfileImageKey(key);

        // 응답용 URL: presigned로 내려주는 걸 추천
        String presigned = s3Upload.presignedGetUrl(key, Duration.ofMinutes(30));

        // (선택) oldKey가 있으면 s3에서 삭제
        // if (oldKey != null) s3Upload.delete(oldKey);

        return presigned;
    }

    @Transactional
    public void deleteProfilePhoto(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        String oldKey = member.getProfileImageKey();
        member.setProfileImageKey(null);

        // (선택) S3에서도 삭제
        // if (oldKey != null) s3Upload.delete(oldKey);
    }
}
