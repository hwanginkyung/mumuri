package growdy.mumuri.service;

import growdy.mumuri.domain.Couple;
import growdy.mumuri.domain.Member;
import growdy.mumuri.domain.Schedule;
import growdy.mumuri.domain.ScheduleOwnerType;
import growdy.mumuri.dto.ScheduleCreateRequest;
import growdy.mumuri.dto.ScheduleResponse;
import growdy.mumuri.login.repository.MemberRepository;
import growdy.mumuri.repository.CoupleRepository;
import growdy.mumuri.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final CoupleRepository coupleRepository;

    /**
     * 일정 생성
     */
    @Transactional
    public ScheduleResponse create(Long memberId, ScheduleCreateRequest req) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Couple couple = null;
        if (req.couple()) {
            couple = coupleRepository
                    .findByMember1IdOrMember2Id(memberId, memberId)
                    .orElseThrow(() -> new IllegalStateException("커플이 아닙니다."));
        }

        LocalDate date = req.date();
        LocalDateTime startAt;
        LocalDateTime endAt;

        if (req.allDay()) {
            startAt = date.atStartOfDay();
            endAt = date.atTime(LocalTime.MAX); // 하루종일
        } else {
            startAt = LocalDateTime.of(date, req.startTime());
            endAt = LocalDateTime.of(date, req.endTime());
        }

        Schedule schedule = Schedule.builder()
                .owner(me)
                .couple(couple)
                .title(req.title())
                .startAt(startAt)
                .endAt(endAt)
                .allDay(req.allDay())
                .build();

        Schedule saved = scheduleRepository.save(schedule);

        return toResponse(saved, me.getId(), getPartnerId(me.getId()));
    }

    /**
     * 월 단위 조회 (캘린더 화면용)
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMonthly(Long memberId, int year, int month) {
        Member me = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(memberId, memberId)
                .orElse(null);

        Long partnerId = getPartnerId(memberId, couple);

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        LocalDateTime from = firstDay.atStartOfDay();
        LocalDateTime to = lastDay.atTime(LocalTime.MAX);

        // 내 일정
        List<Schedule> mySchedules =
                scheduleRepository.findByOwnerIdAndStartAtBetween(memberId, from, to);

        // 커플 일정
        List<Schedule> coupleSchedules = new ArrayList<>();
        if (couple != null) {
            coupleSchedules = scheduleRepository
                    .findByCoupleIdAndStartAtBetween(couple.getId(), from, to);
        }

        // 애인 개인 일정
        List<Schedule> partnerSchedules = new ArrayList<>();
        if (partnerId != null) {
            partnerSchedules = scheduleRepository
                    .findByOwnerIdAndStartAtBetween(partnerId, from, to);
        }

        // 다 합치고 정렬 + 타입 계산
        List<Schedule> all = new ArrayList<>();
        all.addAll(mySchedules);
        all.addAll(coupleSchedules);
        all.addAll(partnerSchedules);

        all.sort(Comparator.comparing(Schedule::getStartAt));

        return all.stream()
                .map(s -> toResponse(s, memberId, partnerId))
                .toList();
    }

    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        // 내 일정이거나, 커플 일정(둘 다 수정 가능)만 삭제 허용
        if (!schedule.getOwner().getId().equals(memberId) &&
                (schedule.getCouple() == null ||
                        !isMyCouple(memberId, schedule.getCouple()))) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        scheduleRepository.delete(schedule);
    }

    // ====== 헬퍼 ======

    private Long getPartnerId(Long myId) {
        Couple couple = coupleRepository
                .findByMember1IdOrMember2Id(myId, myId)
                .orElse(null);
        return getPartnerId(myId, couple);
    }

    private Long getPartnerId(Long myId, Couple couple) {
        if (couple == null) return null;
        if (couple.getMember1() != null && couple.getMember1().getId().equals(myId)) {
            return couple.getMember2() != null ? couple.getMember2().getId() : null;
        }
        if (couple.getMember2() != null && couple.getMember2().getId().equals(myId)) {
            return couple.getMember1() != null ? couple.getMember1().getId() : null;
        }
        return null;
    }

    private boolean isMyCouple(Long myId, Couple couple) {
        return couple.getMember1() != null && couple.getMember1().getId().equals(myId)
                || couple.getMember2() != null && couple.getMember2().getId().equals(myId);
    }

    private ScheduleResponse toResponse(Schedule s, Long myId, Long partnerId) {
        ScheduleOwnerType type;
        if (s.getCouple() != null) {
            type = ScheduleOwnerType.TOGETHER;
        } else if (s.getOwner().getId().equals(myId)) {
            type = ScheduleOwnerType.ME;
        } else {
            type = ScheduleOwnerType.PARTNER;
        }

        return new ScheduleResponse(
                s.getId(),
                s.getTitle(),
                s.getStartAt(),
                s.getEndAt(),
                s.isAllDay(),
                s.getCouple() != null,
                type
        );
    }
}