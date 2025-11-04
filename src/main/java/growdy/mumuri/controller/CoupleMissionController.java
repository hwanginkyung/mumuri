package growdy.mumuri.controller;

import growdy.mumuri.domain.CoupleMission;
import growdy.mumuri.dto.CoupleMissionDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.CoupleMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/couples/missions")
public class CoupleMissionController {

    private final CoupleMissionService coupleMissionService;

    @GetMapping("/today")
    public List<CoupleMissionDto> today(@AuthenticationPrincipal CustomUserDetails user) {
        List<CoupleMission> list = coupleMissionService.getTodayMissions(user.getId());
        return list.stream().map(CoupleMissionDto::from).toList();
    }

    @PostMapping("/{missionId}/complete")
    public void completeMyPart(@PathVariable Long missionId,
                               @RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal CustomUserDetails user) {
        coupleMissionService.completeMyPart(user.getId(), missionId, file);
    }
}
