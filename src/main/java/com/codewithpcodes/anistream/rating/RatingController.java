package com.codewithpcodes.anistream.rating;

import com.codewithpcodes.anistream.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media/{media-id}/ratings")
@Tag(name = "Ratings", description = "User ratings for anime")
public class RatingController {

    public final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> submitRating(
            @PathVariable("media-id") UUID mediaId,
            @AuthenticationPrincipal User user,
            @RequestBody SubmitRatingRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ratingService.submitRating(mediaId, user, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteRating(
            @PathVariable("media-id") UUID mediaId,
            @AuthenticationPrincipal User user
    ) {
        ratingService.deleteRating(mediaId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<RatingResponse>> getRatings(
            @PathVariable("media-id") UUID mediaId,
            int page,
            int size
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ratingService.getRatingsForMedia(mediaId, page, size));
    }

    @GetMapping("/mine")
    public ResponseEntity<RatingResponse> getMyRating(
            @PathVariable("media-id") UUID mediaId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ratingService.getUserRatingForMedia(mediaId, user.getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCountRatings(
            @PathVariable("media-id") UUID mediaId
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ratingService.countRatingsForMedia(mediaId));
    }
}
