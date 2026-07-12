package com.codewithpcodes.anistream.recommendation;

import com.codewithpcodes.anistream.media.MediaContent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecommendationResponse(
        UUID mediaId,
        String title,
        String titleEnglish,
        String titleJapanese,
        String description,
        String mediaType,
        List<String> genres,
        String thumbnailUrl,
        String bannerUrl,
        String masterPlaylistUrlSub,
        String masterPlaylistUrlDub,
        int malId,
        String airingStatus,
        int totalEpisodes,
        String season,
        int seasonYear,
        String studio,
        LocalDate releaseDate,
        BigDecimal avgRating,
        int totalRatings,
        boolean hasSub,
        boolean hasDub,
        Map<String, Object> metadataPayload
) {
    public static RecommendationResponse from (MediaContent media) {
        return new RecommendationResponse(
                media.getId(),
                media.getTitle(),
                media.getTitleEnglish() != null ? media.getTitleEnglish() : "",
                media.getTitleJapanese() != null ? media.getTitleJapanese() : "",
                media.getDescription() != null ? media.getDescription() : "",
                media.getType().name(),
                media.getGenres(),
                media.getThumbnailUrl() != null ? media.getThumbnailUrl() : "",
                media.getBannerUrl() != null ? media.getBannerUrl() : "",
                media.getMasterPlaylistUrlSub(),
                media.getMasterPlaylistUrlDub(),
                media.getMalId(),
                media.getAiringStatus().name(),
                media.getTotalEpisodes(),
                media.getSeason() != null ? media.getSeason() : "",
                media.getSeasonYear(),
                media.getStudio() != null ? media.getStudio() : "",
                media.getReleaseDate(),
                media.getAvgRating(),
                media.getTotalRatings(),
                media.getHasSub(),
                media.getHasDub(),
                media.getMetadataPayload()
        );
    }
}
