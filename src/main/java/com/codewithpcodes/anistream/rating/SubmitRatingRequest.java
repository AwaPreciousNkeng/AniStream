package com.codewithpcodes.anistream.rating;

public record SubmitRatingRequest(
        int score,
        String review
) {
}
