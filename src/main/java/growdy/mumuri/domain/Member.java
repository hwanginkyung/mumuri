package growdy.mumuri.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long kakaoId;
    private String appleId;
    private String name;
    private String email;
    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate birthday;
    private String nickname;
    @JsonFormat(pattern = "yyyy.MM.dd")
    private LocalDate anniversary;
    @Column(nullable = false)
    private String status = "solo";
    private String password;
    @OneToOne(mappedBy = "member1")
    @JsonIgnore
    private Couple couple;
    private String coupleCode;
    private boolean deleted = false;
/*    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Photo> photos;*/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_photo_id") // nullable
    private Photo mainPhoto;
    private String profileImageKey;

}
