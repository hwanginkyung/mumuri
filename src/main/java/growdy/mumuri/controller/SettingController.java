package growdy.mumuri.controller;

import growdy.mumuri.domain.Member;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/setting")
public class SettingController {
    private final UserSettingService userSettingService;
    @PostMapping(value = "/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateProfilePhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart MultipartFile file
    ) {
        Long memberId = AuthGuard.requireUser(user).getId();
        String url = userSettingService.updateProfilePhoto(memberId, file);
        return ResponseEntity.ok(url); // 프론트가 바로 반영 가능
    }

    @DeleteMapping("/profile-photo")
    public ResponseEntity<Void> deleteProfilePhoto(@AuthenticationPrincipal CustomUserDetails user) {
        userSettingService.deleteProfilePhoto(AuthGuard.requireUser(user).getId());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/name")
    public ResponseEntity<String> UpdateName(@AuthenticationPrincipal CustomUserDetails user,
                                             @RequestParam String name) {
        Long memberId= AuthGuard.requireUser(user).getId();
        userSettingService.updateMemberName(memberId, name);
        return ResponseEntity.ok(name);
    }

    @PostMapping("/birthday")
    public ResponseEntity<String> UpdateBirthday(@AuthenticationPrincipal CustomUserDetails user,
                                                 @RequestParam(required = false) String birthday) {
        Long memberId= AuthGuard.requireUser(user).getId();
        LocalDate parsedBirthday = StringUtils.hasText(birthday) ? LocalDate.parse(birthday) : null;
        userSettingService.updateMemberBirthday(memberId, parsedBirthday);
        return ResponseEntity.ok(parsedBirthday != null ? parsedBirthday.toString() : null);
    }
    @PostMapping("/anniversary")
    public ResponseEntity<String> updateAnniversary(@AuthenticationPrincipal CustomUserDetails user,
                                                    @RequestParam LocalDate anniversary) {
        Long memberId = AuthGuard.requireUser(user).getId();
        userSettingService.updateMemberAnniversary(memberId, anniversary);
        return ResponseEntity.ok(anniversary.toString());
    }
}
