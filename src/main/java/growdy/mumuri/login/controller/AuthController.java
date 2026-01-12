package growdy.mumuri.login.controller;

import growdy.mumuri.dto.LogoutRequest;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.login.dto.TokenResponse;
import growdy.mumuri.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        TokenResponse res = authService.refresh(refreshToken);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody LogoutRequest req
    ) {
        authService.logout(AuthGuard.requireUser(user).getId(), req.refreshToken());
        return ResponseEntity.ok().build();
    }
}
