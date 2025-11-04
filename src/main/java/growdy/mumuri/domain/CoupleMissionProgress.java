package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "couple_mission_progress",
        uniqueConstraints = @UniqueConstraint(name="uq_progress_one_per_user", columnNames={"couple_mission_id","userId"}))
@Getter
@Setter
public class CoupleMissionProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CoupleMission coupleMission;

    @Column(nullable = false)
    private Long userId;

    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProgressStatus status = ProgressStatus.NOT_DONE;

    private Instant completedAt;

    protected CoupleMissionProgress() {
    }

    public CoupleMissionProgress(CoupleMission cm, Long userId) {
        this.coupleMission = cm;
        this.userId = userId;
        cm.addProgress(this);
    }

    public void complete(String photoUrl) {
        this.photoUrl = photoUrl;
        this.status = ProgressStatus.DONE;
        this.completedAt = Instant.now();
    }
}
