package growdy.mumuri.controller;


import growdy.mumuri.dto.MyPageDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService myPageService;

    @GetMapping("/api/mypage")
    public ResponseEntity<MyPageDto> mypage(
            @AuthenticationPrincipal CustomUserDetails user){
        MyPageDto mypages= myPageService.mypage(user);
        return ResponseEntity.ok(mypages);
    }
}
