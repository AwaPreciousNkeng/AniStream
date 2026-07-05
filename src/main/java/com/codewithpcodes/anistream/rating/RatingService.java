package com.codewithpcodes.anistream.rating;

import com.codewithpcodes.anistream.exceptions.BadRequestException;
import com.codewithpcodes.anistream.exceptions.ResourceNotFoundException;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    public final RatingRepository ratingRepository;
    public final MediaRepository mediaRepository;

    @Transactional
    public RatingResponse submitRating(UUID mediaId, User user, SubmitRatingRequest request) {
        if (request.score() < 1 || request.score() > 10) {
            throw new BadRequestException("Rating score must be between 1 and 10");
        }

        MediaContent media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media Not Found"));

        Rating rating = ratingRepository.findByUserIdAndMediaId(user.getId(), mediaId)
                .orElse(Rating.builder()
                        .media(media)
                        .user(user)
                        .build());

        rating.setScore(request.score());
        rating.setReview(request.review());
        ratingRepository.save(rating);

        calculateAvgRating(media);

        return RatingResponse.from(rating);
    }

    @Transactional
    public void deleteRating(UUID mediaId, User user) {
        Rating rating = ratingRepository.findByUserIdAndMediaId(user.getId(), mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating Not Found"));

        ratingRepository.delete(rating);

        MediaContent media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media Not Found"));
        calculateAvgRating(media);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsForMedia(UUID mediaId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ratingRepository.findByMediaIdOrderByCreatedAtDesc(mediaId, pageable)
                .stream()
                .map(RatingResponse::from)
                .toList();
    }

    public long countRatingsForMedia(UUID mediaId) {
        return ratingRepository.countByMediaId(mediaId);
    }

    @Transactional(readOnly = true)
    public RatingResponse getUserRatingForMedia(UUID mediaId, UUID userId) {
        return ratingRepository.findByUserIdAndMediaId(userId, mediaId)
                .map(RatingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Rating Not Found"));
    }

    private void calculateAvgRating(MediaContent media) {
        Double avg = ratingRepository.computedAvgRating(media.getId());
        media.setAvgRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        mediaRepository.save(media);
    }
}
