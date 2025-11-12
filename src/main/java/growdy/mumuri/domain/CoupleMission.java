package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name="uq_couple_mission_day", columnNames={"couple_id","mission_id","missionDate"}))
@Getter
@Setter
public class CoupleMission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Mission mission;

    @Column(nullable = false)
    private LocalDate missionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MissionStatus status = MissionStatus.NOT_STARTED;

    private Instant completedAt;

    @OneToMany(mappedBy = "coupleMission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoupleMissionProgress> progresses = new ArrayList<>();



    protected CoupleMission() {}
    public CoupleMission(Couple couple, Mission mission, LocalDate date) {
        this.couple = couple; this.mission = mission; this.missionDate = date;

    }

    public void updateStatusByProgress() {
        long doneCount = progresses.stream().filter(p -> p.getStatus() == ProgressStatus.DONE).count();
        if (doneCount == 0) status = MissionStatus.NOT_STARTED;
        else if (doneCount == 1) status = MissionStatus.HALF_DONE;
        else {
            status = MissionStatus.COMPLETED;
            completedAt = Instant.now();
        }
    }

    // package-private helper
    void addProgress(CoupleMissionProgress p) { this.progresses.add(p); }
}
