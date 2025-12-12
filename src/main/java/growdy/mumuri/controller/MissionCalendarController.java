package growdy.mumuri.controller;

import growdy.mumuri.dto.MissionDaySummaryDto;
import growdy.mumuri.dto.MissionDetailDto;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.MissionCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar/missions")
public class MissionCalendarController {

    private final MissionCalendarService missionCalendarService;

    /**
     *  🗓 미션 캘린더 월 데이터
     *  GET /calendar/missions?year=2025&month=10
     */
    @GetMapping
    public ResponseEntity<List<MissionDaySummaryDto>> getMonthly(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<MissionDaySummaryDto> res =
                missionCalendarService.getMonthly(user.getId(), year, month);
        return ResponseEntity.ok(res);
    }

    /**
     *  📸 특정 날짜 상세
     *  GET /calendar/missions/day?date=2025-10-25
     */
    @GetMapping("/day")
    public ResponseEntity<List<MissionDetailDto>> getDaily(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam LocalDate date
    ) {
        List<MissionDetailDto> res =
                missionCalendarService.getDaily(user.getId(), date);
        return ResponseEntity.ok(res);
    }
}
