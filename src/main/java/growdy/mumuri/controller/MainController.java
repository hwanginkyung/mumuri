package growdy.mumuri.controller;


import growdy.mumuri.dto.HomeDto;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MainController {

    private final MainService mainService;
    @GetMapping("/home/main")
    public ResponseEntity<HomeDto> homeDto(@AuthenticationPrincipal CustomUserDetails user) {
        HomeDto home= mainService.getHome(AuthGuard.requireUser(user));
        return ResponseEntity.ok(home);
    }
}
