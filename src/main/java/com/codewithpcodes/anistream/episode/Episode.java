package com.codewithpcodes.anistream.episode;

import com.codewithpcodes.anistream.media.MediaContent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent series;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "master_playlist_url", length = 512)
    private String masterPlaylistUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;
}
