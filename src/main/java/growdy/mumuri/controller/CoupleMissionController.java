package growdy.mumuri.controller;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.dto.CoupleMissionDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.CoupleMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples/missions")
public class CoupleMissionController {

    private final CoupleMissionService coupleMissionService;

    @GetMapping("/today")
    public List<CoupleMissionDto> today(@AuthenticationPrincipal CustomUserDetails user) {
        List<CoupleMission> list = coupleMissionService.getTodayMissions(user.getId());
        return list.stream()
                .map(cm -> CoupleMissionDto.from(cm, user.getId())) // userId 전달!!
                .toList();
    }

    @PostMapping("/{missionId}/complete")
    public ResponseEntity<Instant> completeMyPart(@PathVariable Long missionId,
                                                 @RequestParam("file") MultipartFile file,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        Instant now= coupleMissionService.completeMyPart(user.getId(), missionId, file);
        return ResponseEntity.ok(now);
    }
    @PostMapping(value = "/{missionId}/complete-v2", consumes = "application/json")
    public Instant completeMyPartJson(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long missionId,
            @RequestBody Map<String, String> body
    ) {
        return coupleMissionService.completeWithUrl(user.getId(), missionId, body.get("file"));
    }
}
