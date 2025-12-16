package growdy.mumuri.controller;

import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class SettingController {
    private final UserSettingService userSettingService;
    @PostMapping(value = "/profile-photo", consumes = "multipart/form-data")
    public ResponseEntity<String> updateProfilePhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestPart MultipartFile file
    ) {
        Long memberId = user.getId();
        String url = userSettingService.updateProfilePhoto(memberId, file);
        return ResponseEntity.ok(url); // 프론트가 바로 반영 가능
    }

    @DeleteMapping("/profile-photo")
    public ResponseEntity<Void> deleteProfilePhoto(@AuthenticationPrincipal CustomUserDetails user) {
        userSettingService.deleteProfilePhoto(user.getId());
        return ResponseEntity.ok().build();
    }
}
