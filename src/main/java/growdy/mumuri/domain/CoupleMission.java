package growdy.mumuri.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
public class CoupleMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Mission mission;

    @Column(nullable = false)
    private LocalDate missionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MissionStatus status = MissionStatus.NOT_STARTED;

    private Instant completedAt;

    @OneToMany(mappedBy = "coupleMission",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<CoupleMissionProgress> progresses = new ArrayList<>();

    protected CoupleMission() {
    }

    public CoupleMission(Couple couple, Mission mission, LocalDate date) {
        this.couple = couple;
        this.mission = mission;
        this.missionDate = date;
    }

    /**
     * 양방향 편의 메서드
     */
    public void addProgress(CoupleMissionProgress progress) {
        progresses.add(progress);
        progress.setCoupleMission(this);
    }

    public void updateStatusByProgress() {
        if (progresses == null || progresses.isEmpty()) {
            status = MissionStatus.NOT_STARTED;
            completedAt = null;
            return;
        }

        long doneCount = progresses.stream()
                .filter(p -> p != null && p.getStatus() == ProgressStatus.DONE)
                .count();

        if (doneCount == 0) {
            status = MissionStatus.NOT_STARTED;
            completedAt = null;
        } else if (doneCount == 1) {
            status = MissionStatus.HALF_DONE;
            completedAt = null;
        } else {
            status = MissionStatus.COMPLETED;
            completedAt = Instant.now();
        }
    }
}