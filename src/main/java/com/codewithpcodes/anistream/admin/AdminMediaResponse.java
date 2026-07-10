package com.codewithpcodes.anistream.admin;

import com.codewithpcodes.anistream.media.MediaContent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminMediaResponse(
        UUID id,
        String title,
        String mediaType,
        String airingStatus,
        BigDecimal avgRating,
        int episodeCount,
        Instant createdAt
) {
    public static AdminMediaResponse from(MediaContent media) {
        return new AdminMediaResponse(
                media.getId(),
                media.getTitle(),
                media.getType().name(),
                media.getAiringStatus().name(),
                media.getAvgRating(),
                media.getTotalEpisodes(),
                media.getCreatedAt()
        );
    }
}
