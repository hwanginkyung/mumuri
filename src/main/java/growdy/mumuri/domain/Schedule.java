package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member owner;     // 이 일정을 만든 사람(청월 or 애인)

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;    // 커플 일정이면 셋팅, 개인 일정이면 null

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean allDay;   // 하루종일 체크박스

    public boolean isCoupleSchedule() {
        return couple != null;
    }

    public void update(String title, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, Couple couple) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.couple = couple;
    }
}

