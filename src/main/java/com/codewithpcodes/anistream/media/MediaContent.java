package com.codewithpcodes.anistream.media;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "media_content")
public class MediaContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "title_japanese")
    private String titleJapanese;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;

    @ElementCollection
    @CollectionTable(
            name = "media_genres",
            joinColumns = @JoinColumn(name = "media_id")
    )
    @Column(name = "genre")
    @Builder.Default
    private List<String> genres = new ArrayList<>();

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Column(name = "master_playlist_url_sub", length = 512)
    private String masterPlaylistUrlSub;

    @Column(name = "master_playlist_url_dub", length = 512)
    private String masterPlaylistUrlDub;

    @Column(name = "anilist_id")
    private Integer anilistId;

    @Column(name = "mal_id")
    private Integer malId;

    @Enumerated(EnumType.STRING)
    @Column(name = "airing_status")
    private AiringStatus airingStatus;

    private Integer totalEpisodes;

    @Column(name = "season", length = 50)
    private String season;

    @Column(name = "season_year")
    private Integer seasonYear;

    private String studio;

    private LocalDate releaseDate;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "has_sub", nullable = false)
    @Builder.Default
    private Boolean hasSub = true;

    @Column(name = "has_dub", nullable = false)
    @Builder.Default
    private Boolean hasDub = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_payload", columnDefinition = "jsonb")
    private Map<String, Object> metadataPayload;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;
}
