package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Setter
public class CouplePhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String photoUrl; // S3 URL

    @ManyToOne
    private Couple couple;
}
