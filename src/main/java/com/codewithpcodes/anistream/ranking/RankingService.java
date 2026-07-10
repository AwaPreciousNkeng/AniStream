package com.codewithpcodes.anistream.ranking;

import com.codewithpcodes.anistream.media.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final MediaRepository mediaRepository;
    private final MediaViewCountRepository mediaViewCountRepository;

    @Transactional(readOnly = true)
    public List<RankedMediaResponse> getTrending(int limit) {
        LocalDate since = LocalDate.now().minusDays(7);
        return mediaViewCountRepository.findTopMediaInRange(since, LocalDate.now(), limit)
                .stream()
                .map(RankedMediaResponse::fromViewResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RankedMediaResponse> getMostWatched(int limit) {
        return mediaViewCountRepository
                .findMostWatchedAllTime(PageRequest.of(0, limit))
                .stream()
                .map(RankedMediaResponse::fromViewResult)
                .toList();
    }

    @Transactional
    public void recordView(UUID mediaId, UUID episodeId, UUID userId) {
        mediaViewCountRepository.incrementViewCount(mediaId, episodeId, userId);
    }

    @Transactional(readOnly = true)
    public List<RankedMediaResponse> getTopRated(int limit) {
        return mediaRepository
                .findTopRatedWithMinimumRatings(10, PageRequest.of(0, limit))
                .stream()
                .map(RankedMediaResponse::from)
                .toList();
    }


}
