package com.codewithpcodes.anistream.ranking;

import com.codewithpcodes.anistream.media.MediaContent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "rankings")
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RankingPeriod period;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Integer viewCount;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @CreationTimestamp
    @Column(name = "computed_at", updatable = false)
    private LocalDateTime computedAt;
}
