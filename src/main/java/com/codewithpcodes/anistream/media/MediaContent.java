package com.codewithpcodes.anistream.media;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;

    @Column(length = 50)
    private String genre;

    private String masterPlaylistUrl;

    private String thumbnailUrl;

    private String rawFilePath;

    private LocalDate releaseDate;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_payload", columnDefinition = "jsonb")
    private Map<String, Object> metadataPayload;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;
}
