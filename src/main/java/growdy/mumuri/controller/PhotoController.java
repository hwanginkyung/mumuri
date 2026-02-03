package growdy.mumuri.controller;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.dto.PhotoResponseDto;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.CoupleService;
import growdy.mumuri.service.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@Slf4j
@RestController
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoService photoService;
    private final CoupleService coupleService;
    @PostMapping("/photo/{couple_id}")
    public ResponseEntity<String> uploadPhoto(
            @PathVariable long couple_id,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long missionId,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        String fileKey = photoService.uploadPhoto(couple_id, file, AuthGuard.requireUser(user).getId(), missionId);
        return ResponseEntity.ok(fileKey);
    }
    @GetMapping("/photo/{couple_id}/{photo_id}")
    public PhotoResponseDto getPhoto(
            @PathVariable long couple_id,
            @PathVariable long photo_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        return photoService.getOne(couple_id, photo_id, authenticatedUser.getId());
    }
    @GetMapping("/photo/{couple_id}/all")
    public List<PhotoResponseDto> gallery(
            @PathVariable long couple_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        return photoService.listByCouple(couple_id, authenticatedUser.getId());
    }
    @DeleteMapping("/delete/{couple_id}/{photo_id}")
    public ResponseEntity<String> deletePhoto(
            @PathVariable long couple_id,
            @PathVariable long photo_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        AuthGuard.requireUser(user);
        photoService.delete(photo_id,couple_id,true);
        return ResponseEntity.ok("삭제 완");
    }
    @PostMapping("/test")
    public long test(@AuthenticationPrincipal CustomUserDetails user){
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        Couple couple = coupleService.test(authenticatedUser.getUser());
        log.info("couple_id :{}", couple.getId());
        log.info("user_id : {}",authenticatedUser.getId());
        return couple.getId();
    }
    @PostMapping("/test/already")
    public long testAlready(@AuthenticationPrincipal CustomUserDetails user){
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        Couple couple = coupleService.test(authenticatedUser.getUser());
        log.info("couple_id :{}", couple.getId());
        log.info("user_id : {}",authenticatedUser.getId());
        return couple.getId();
    }
    @PostMapping("/test/go")
    public String testGo(@AuthenticationPrincipal CustomUserDetails user){
        CustomUserDetails authenticatedUser = AuthGuard.requireUser(user);
        String code= coupleService.newcode(authenticatedUser.getUser());
        log.info("user_id : {}",authenticatedUser.getId());
        return code;
    }

}
