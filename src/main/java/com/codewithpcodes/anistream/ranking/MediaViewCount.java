package com.codewithpcodes.anistream.ranking;

import com.codewithpcodes.anistream.media.MediaContent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "media_view_counts",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"media_id", "view_date"}
        )
)
public class MediaViewCount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent media;

    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
}
