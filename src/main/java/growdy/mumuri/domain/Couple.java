package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Setter
@Getter
public class Couple extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    private Member member1;
    @OneToOne
    private Member member2;
    private String coupleCode;
    private LocalDate anniversary;
    @OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<CouplePhoto> photos;
    /*@OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<CoupleChat> chats;*/
    @OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<CoupleQuestion> questions;
}
