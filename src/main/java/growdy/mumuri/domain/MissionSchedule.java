package growdy.mumuri.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name="uq_mission_date", columnNames={"mission_id","missionDate"}))
public class MissionSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    private Mission mission;

    @Column(nullable=false)
    private LocalDate missionDate;

    protected MissionSchedule() {}
    public MissionSchedule(Mission mission, LocalDate date) {
        this.mission = mission; this.missionDate = date;
    }
}
