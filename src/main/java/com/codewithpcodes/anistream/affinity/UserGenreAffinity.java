package com.codewithpcodes.anistream.affinity;

import com.codewithpcodes.anistream.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_genre_affinity")
public class UserGenreAffinity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(name = "affinity_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal affinityScore = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
