package growdy.mumuri.controller;

import growdy.mumuri.dto.MissionDetailDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GalleryController {
    private final PhotoService photoService;
    @GetMapping("/photos/gallery")
    public ResponseEntity<Page<MissionDetailDto>> getGallery(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(photoService.getGallery(user.getId(), page));
    }
}
