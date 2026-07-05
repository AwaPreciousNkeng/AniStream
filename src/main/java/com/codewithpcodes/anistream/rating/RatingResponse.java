package com.codewithpcodes.anistream.rating;

import java.time.LocalDateTime;
import java.util.UUID;

public record RatingResponse(
        UUID id,
        UUID mediaId,
        UUID userId,
        String username,
        int score,
        String review,
        LocalDateTime createdAt
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getMedia().getId(),
                rating.getUser().getId(),
                rating.getUser().getUsername(),
                rating.getScore(),
                rating.getReview(),
                rating.getCreatedAt()
        );
    }
}
