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

    @Column(nullable = false)
    private String title;

    @Column(name = "title_japanese", nullable = false)
    private String titleJapanese;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "master_playlist_url_sub", length = 512)
    private String masterPlaylistUrlSub;

    @Column(name = "master_playlist_url_dub", length = 512)
    private String masterPlaylistUrlDub;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "sub_available")
    @Builder.Default
    private Boolean subAvailable = false;

    @Column(name = "dub_available")
    @Builder.Default
    private Boolean dubAvailable = false;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;
}
