package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Column(length = 500, unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private Instant expiryAt;

    // 옵션: 디바이스 구분, user-agent, ip 등
    private String userAgent;
    private String ip;
}
