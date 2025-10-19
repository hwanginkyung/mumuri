package growdy.mumuri.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Builder
@AllArgsConstructor
public class Photo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String s3Key;             // S3 파일 경로
    private String url;               // S3 정적 URL
    private String description;       // 사진 설명( 질문 번호? ,.,)
    private Long uploadedBy;          // 업로더 (유저 ID)
    @ManyToOne(fetch = FetchType.LAZY)
    private Couple couple;
    @Column(nullable = false)
    private boolean deleted;
    public void softDelete() {
        this.deleted = true;
    }
}
