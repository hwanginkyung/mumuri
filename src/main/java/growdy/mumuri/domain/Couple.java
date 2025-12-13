package growdy.mumuri.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Builder
@AllArgsConstructor
public class Couple extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne
    @JsonIgnore
    private Member member1;
    @OneToOne
    @JsonIgnore
    private Member member2;
    private String coupleCode;
    private LocalDate anniversary;
    @OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<Photo> photos;
    /*@OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<CoupleChat> chats;*/
    @OneToMany(mappedBy = "couple", cascade = CascadeType.ALL)
    private List<CoupleMission> questions;
}


