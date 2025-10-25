package growdy.mumuri.controller;

import growdy.mumuri.domain.ChatRoom;
import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.PhotoResponseDto;
import growdy.mumuri.dto.TestDto;
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
            @AuthenticationPrincipal CustomUserDetails user
            ){
        photoService.uploadPhoto(couple_id, file, user.getId());
        return ResponseEntity.ok("photo updated successfully");
    }
    @GetMapping("/photo/{couple_id}/{photo_id}")
    public PhotoResponseDto getPhoto(
            @PathVariable long couple_id,
            @PathVariable long photo_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        return photoService.getOne(photo_id,couple_id);
    }
    @GetMapping("/photo/{couple_id}/all")
    public List<PhotoResponseDto> gallery(
            @PathVariable long couple_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        return photoService.listByCouple(couple_id);
    }
    @DeleteMapping("/delete/{couple_id}/{photo_id}")
    public ResponseEntity<String> deletePhoto(
            @PathVariable long couple_id,
            @PathVariable long photo_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        photoService.delete(photo_id,couple_id,true);
        return ResponseEntity.ok("삭제 완");
    }
    @PostMapping("/test")
    public long test(@AuthenticationPrincipal CustomUserDetails user){
        Couple couple = coupleService.test(user.getUser());
        log.info("couple_id :{}", couple.getId());
        log.info("user_id : {}",user.getId());
        return couple.getId();
    }
    @PostMapping("/test/already")
    public long testAlready(@AuthenticationPrincipal CustomUserDetails user){
        Couple couple = coupleService.test(user.getUser());
        log.info("couple_id :{}", couple.getId());
        log.info("user_id : {}",user.getId());
        return couple.getId();
    }
    @PostMapping("/test/go")
    public String testGo(@AuthenticationPrincipal CustomUserDetails user){
        String code= coupleService.newcode(user.getUser());
        log.info("user_id : {}",user.getId());
        return code;
    }

}
