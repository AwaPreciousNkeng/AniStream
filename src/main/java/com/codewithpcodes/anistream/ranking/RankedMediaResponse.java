package com.codewithpcodes.anistream.ranking;

import com.codewithpcodes.anistream.media.MediaContent;

import java.math.BigDecimal;
import java.util.UUID;

public record RankedMediaResponse(
        UUID id,
        String title,
        String thumbnailUrl,
        BigDecimal avgRating,
        long viewCount
) {
    public static RankedMediaResponse from(MediaContent media) {
        return new RankedMediaResponse(
                media.getId(),
                media.getTitle(),
                media.getThumbnailUrl(),
                media.getAvgRating(),
                0L
        );
    }

    public static RankedMediaResponse fromViewResult(Object[] result) {
        MediaContent media = (MediaContent) result[0];
        long viewCount = ((Number) result[1]).longValue();
        return new RankedMediaResponse(
                media.getId(),
                media.getTitle(),
                media.getThumbnailUrl(),
                media.getAvgRating(),
                viewCount
        );
    }
}
