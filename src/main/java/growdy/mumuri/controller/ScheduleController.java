package growdy.mumuri.controller;

import growdy.mumuri.dto.ScheduleCreateRequest;
import growdy.mumuri.dto.ScheduleResponse;
import growdy.mumuri.login.AuthGuard;
import growdy.mumuri.login.CustomUserDetails;
import growdy.mumuri.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/calendar/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 일정 생성
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ScheduleCreateRequest req
    ) {
        ScheduleResponse res = scheduleService.create(AuthGuard.requireUser(user).getId(), req);
        return ResponseEntity.ok(res);

    }

    // 특정 월 일정 조회 (캘린더 화면)
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getMonthlySchedules(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<ScheduleResponse> res = scheduleService.getMonthly(AuthGuard.requireUser(user).getId(), year, month);
        return ResponseEntity.ok(res);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        scheduleService.delete(AuthGuard.requireUser(user).getId(), id);
        return ResponseEntity.noContent().build();
    }
}
