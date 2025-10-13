package growdy.mumuri.domain;

import jakarta.persistence.*;

@Entity
public class CoupleQuestion extends BaseEntity {
    @Id @GeneratedValue
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String content;
    @ManyToOne
    private Couple couple;
}
