package growdy.mumuri.controller;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.dto.PhotoResponseDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoService photoService;
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
    public ResponseEntity<String> getPhoto(
            @PathVariable long couple_id,
            @PathVariable long photo_id,
            @AuthenticationPrincipal CustomUserDetails user
    ){
        photoService.getOne(photo_id,couple_id);
        return ResponseEntity.ok("photo updated successfully");
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
    @PostMapping("test")
    public ResponseEntity<String> test(@AuthenticationPrincipal CustomUserDetails user){
        Member member = new Member();
        member.setId(user.getId());
        Couple couple = new Couple();
        couple.setMember1(member);
        couple.setMember2(user.getUser());
        return ResponseEntity.ok("임시 커플 만들기 완");
    }
}
