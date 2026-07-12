package com.codewithpcodes.anistream.recommendation;

import com.codewithpcodes.anistream.affinity.UserGenreAffinityRepository;
import com.codewithpcodes.anistream.history.WatchHistoryRepository;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserGenreAffinityRepository affinityRepository;
    private final MediaRepository mediaRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "recommendations:";
    private static final long TTL_MINUTES = 30;

    public List<RecommendationResponse> getRecommendations(UUID userId, int limit) {

        if (limit <= 0) {
            return Collections.emptyList();
        }

        //check redis first
        String key = CACHE_KEY_PREFIX + userId;
        try {
            Object cachedRaw = redisTemplate.opsForValue().get(key);
            if (cachedRaw != null) {
                List<MediaContent> cachedList;
                if (cachedRaw instanceof String) {
                    cachedList = objectMapper.readValue((String) cachedRaw, new TypeReference<List<MediaContent>>() {
                    });
                } else {
                    cachedList = (List<MediaContent>) cachedRaw;
                }
                return cachedList.stream()
                        .limit(limit)
                        .map(RecommendationResponse::from)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Redis cache read error for user {}", userId, e);
        }

        //cache miss - run algorithm
        List<MediaContent> results = computeRecommendations(userId);

        if (results != null && !results.isEmpty()) {
            try {
                String jsonString = objectMapper.writeValueAsString(results);
                redisTemplate.opsForValue().set(key, jsonString,  TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("Failed to serialize recommendations to Redis for user {}", userId, e);
            }
        }

        assert results != null;
        return results.stream()
                    .limit(limit)
                    .map(RecommendationResponse::from)
                    .toList();

    }

    private List<MediaContent> computeRecommendations(UUID userId) {
        List<MediaContent> recommendations = mediaRepository.getTopRecommendations(userId);
        if (recommendations.isEmpty()) {
            return mediaRepository.findTop20ByOrderByAvgRatingDesc();
        }
        return recommendations;
    }

    //Invalidate when user watches something
    public void invalidateForUser(UUID userId) {
        redisTemplate.delete(CACHE_KEY_PREFIX + userId);
    }

    //Invalidate for multiple users (new episode release)
    public void invalidateForUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        List<String> keys = userIds.stream()
                .map(id -> CACHE_KEY_PREFIX + id)
                .collect(Collectors.toList());

        redisTemplate.delete(keys);
    }
}
