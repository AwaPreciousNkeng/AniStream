package com.codewithpcodes.anistream.crawler;

import com.codewithpcodes.anistream.request.AnimeRequest;
import com.codewithpcodes.anistream.request.AnimeRequestRepository;
import com.codewithpcodes.anistream.request.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final AnilistMetadataCrawler metadataCrawler;
    private final AnimeRequestRepository animeRequestRepository;

    /*--- Fetch Trending Anime Daily ------------------
    * Runs every day at 3am
    * Keeps catalogue fresh with new/trending anime
    */
    @Scheduled(cron = "0 0 3 * * *")
    public void fetchTrendingAnime() {
        log.info("Starting daily trending anime fetch...");
        try {
            metadataCrawler.fetchAndSaveTrending(3); // 3 pages ~ 150 anime
            log.info("Daily trending fetch complete");
        } catch (Exception e) {
            log.error("Trending fetch failed: {}", e.getMessage());
        }
    }

    /*--- Process User Requests ----------------------
    * Runs every hour
    * Picks up most voted pending requests
    */
    @Scheduled(fixedDelay = 3_600_000)
    public void processAnimeRequests() {
        log.info("Processing pending anime requests...");

        List<AnimeRequest> pending = animeRequestRepository
                .findByStatusOrderByVoteCountDesc(RequestStatus.PENDING);

        for (AnimeRequest request : pending) {
            try {
                log.info("Processing request: {}", request.getAnimeTitle());

                //Mark as in progress
                request.setStatus(RequestStatus.IN_PROGRESS);
                animeRequestRepository.save(request);

                //Fetch metadata and queue download
                if (request.getAnilistId() != null) {
                    metadataCrawler.fetchAndSaveByAnilistId(request.getAnilistId());
                } else {
                    metadataCrawler.fetchAndSaveByTitle(request.getAnimeTitle());
                }

                //Mark as available
                request.setStatus(RequestStatus.AVAILABLE);
                animeRequestRepository.save(request);
            } catch (Exception e) {
                log.error("Failed to process request {}: {}", request.getAnimeTitle(), e.getMessage());
                request.setStatus(RequestStatus.PENDING);
                animeRequestRepository.save(request);
            }
        }
    }

    /* --- Check for new Episodes of Ongoing Anime -----------
    * Runs every 6 hours
    * Checks if ongoing anime has new episodes
    */
    @Scheduled(fixedDelay = 21_600_000)
    public void checkForNewEpisodes() {
        log.info("Checking for new episodes on ongoing anime...");
        try {
            metadataCrawler.checkOngoingAnimeForNewEpisodes();
        } catch (Exception e) {
            log.error("New episode check failed: {}", e.getMessage());
        }
    }
}
