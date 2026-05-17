package com.codewithpcodes.anistream.history;

import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "watch_history")
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent media;

    @Column(name = "watched_duration")
    private Integer watchedDuration;

    private Integer totalDuration;

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    private LocalDateTime lastWatchedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;
}
