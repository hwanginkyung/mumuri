package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Mission extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=50)
    private String code;

    @Column(nullable=false, length=100)
    private String title;

    @Lob
    private String description;

    private String category;
    @Column(nullable=false)
    private int reward = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private MissionDifficulty difficulty = MissionDifficulty.NORMAL;

    @Column(nullable=false)
    private boolean active = true;

}
