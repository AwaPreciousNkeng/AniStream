package com.codewithpcodes.anistream.recommendation;

import com.codewithpcodes.anistream.affinity.UserGenreAffinity;
import com.codewithpcodes.anistream.affinity.UserGenreAffinityRepository;
import com.codewithpcodes.anistream.history.WatchHistory;
import com.codewithpcodes.anistream.history.WatchHistoryRepository;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserGenreAffinityRepository affinityRepository;
    private final MediaRepository mediaRepository;
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "recommendations:";
    private static final long TTL_MINUTES = 30;

    public List<MediaContent> getRecommendations(UUID userId) {

        //check redis first
        String key = CACHE_KEY_PREFIX + userId;
        List<MediaContent> cached = (List<MediaContent>) redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        //cache miss - run algorithm
        List<MediaContent> results = computeRecommendations(userId);

        //store in redis with TTL
        redisTemplate.opsForValue().set(key, results, TTL_MINUTES, TimeUnit.MINUTES);

        return results;
    }

    private List<MediaContent> computeRecommendations(UUID userId) {

        // 1. Get user's top genres (35% weight)
        List<UserGenreAffinity> affinities = affinityRepository.findTopGenres(userId);

        //2. Get watch history to avoid recommending already watched
        List<WatchHistory> history = watchHistoryRepository.findByUserIdOrderByLastWatchedAtDesc(userId, Pageable.unpaged()).getContent();

        Set<UUID> watchedIDs = history.stream()
                .map(h -> h.getMedia().getId())
                .collect(Collectors.toSet());

        //3. Score all unwatched media
        List<MediaContent> allMedia = mediaRepository.findAll();
        return allMedia.stream()
                .filter(m -> !watchedIDs.contains(m.getId()))
                .map(m -> Map.entry(m, score(m, affinities)))
                .sorted(Map.Entry.<MediaContent, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(20)
                .collect(Collectors.toList());

    }

    private double score(MediaContent media, List<UserGenreAffinity> affinities) {
        double score = 0.0;

        //Genre affinity - 35%
        score += affinities.stream()
                .filter(a -> a.getGenre().equals(media.getGenre()))
                .mapToDouble(a -> a.getAffinityScore().doubleValue() * 0.35)
                .sum();

        //Average rating - 25%
        score += media.getAvgRating().doubleValue() * 0.25;

        //Recency - 20%
        long daysSinceRelease = ChronoUnit.DAYS.between(
                media.getReleaseDate(), LocalDate.now()
        );

        score += Math.min(0, (365 - daysSinceRelease) / 365.0) * 0.20;

        //Total ratings (popularity) - 20%
        score += Math.min(media.getTotalRatings() / 1000.0, 1.0) * 0.20;

        return score;
    }

    //Invalidate when user watches something
    public void invalidateForUser(UUID userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }

    //Invalidate for multiple users (new episode release)
    public void invalidateForUsers(List<UUID> userIds) {
        userIds.forEach(this::invalidateForUser);
    }
}
