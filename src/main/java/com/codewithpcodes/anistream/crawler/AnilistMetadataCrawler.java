package com.codewithpcodes.anistream.crawler;

import com.codewithpcodes.anistream.episode.Episode;
import com.codewithpcodes.anistream.episode.EpisodeRepository;
import com.codewithpcodes.anistream.media.AiringStatus;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.media.MediaType;
import com.codewithpcodes.anistream.transcode.AudioTrack;
import com.codewithpcodes.anistream.transcode.TranscodeJob;
import com.codewithpcodes.anistream.transcode.TranscodeJobRepository;
import com.codewithpcodes.anistream.transcode.TranscodeStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnilistMetadataCrawler {

    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final VideoDownloadService videoDownloadService;
    private final TranscodeJobRepository transcodeJobRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://graphql.anilist.co")
            .build();

    private static final String ANIME_QUERY = """
        query ($id: Int, $search: String) {
          Media(id: $id, search: $search, type: ANIME) {
            id
            idMal
            title {
              romaji
              english
              native
            }
            description
            genres
            status
            episodes
            duration
            season
            seasonYear
            startDate { year month day }
            coverImage { large extraLarge }
            bannerImage
            studios {
              nodes { name isAnimationStudio }
            }
            averageScore
            episodes
          }
        }
    """;

    private static final String TRENDING_QUERY = """
        query ($page: Int) {
          Page(page: $page, perPage: 50) {
            media(type: ANIME, sort: TRENDING_DESC) {
              id
              idMal
              title {
                romaji
                english
                native
              }
              description
              genres
              status
              episodes
              duration
              season
              seasonYear
              startDate { year month day }
              coverImage { large extraLarge }
              bannerImage
              studios {
                nodes { name isAnimationStudio }
              }
              averageScore
            }
          }
        }
    """;

    private static final String ONGOING_QUERY = """
            query ($page: Int) {
                Page(page: $page, perPage: 50) {
                    media(type: ANIME, status: RELEASING) {
                        id
                        episodes
                   }
                }
            }
            """;

    //Fetch single anime by anilistID
    public MediaContent fetchAndSaveByAnilistId(Integer anilistId) {
        log.info("Fetching anime metadata for anilist ID: {}", anilistId);

        Map<String, Object> variables = Map.of("id", anilistId);
        JsonNode media = executeQuery(ANIME_QUERY, variables)
                .path("data").path("Media");

        return buildAndSaveMediaContent(media);
    }

    public MediaContent fetchAndSaveByTitle(String title) {
        log.info("Fetch anime metadata for title: {}", title);

        Map<String, Object> variables = Map.of("search", title);
        JsonNode media = executeQuery(ANIME_QUERY, variables)
                .path("data").path("Media");

        return buildAndSaveMediaContent(media);
    }

    public List<MediaContent> fetchAndSaveTrending(int pages) {
        List<MediaContent> saved = new ArrayList<>();

        for (int page = 1; page <= pages; page++) {
            log.info("Fetching trending anime page: {}", page);

            Map<String, Object> variables = Map.of("page", page);
            JsonNode results = executeQuery(TRENDING_QUERY, variables)
                    .path("data").path("Page").path("media");

            results.forEach(media -> {
                try {
                    MediaContent content = buildAndSaveMediaContent(media);
                    saved.add(content);
                } catch (Exception e) {
                    log.error("Failed to save anime: {}", e.getMessage());
                }
            });
            // Respect anilist rate limit - 90 request per minute
            sleep();
        }
        return saved;
    }

    public void checkOngoingAnimeForNewEpisodes() {
        // Get all ongoing anime from our DB
        List<MediaContent> ongoingAnime = mediaRepository
                .findByAiringStatus(AiringStatus.ONGOING);

        for (MediaContent anime : ongoingAnime) {
            try {
                // Fetch latest episode count from Anilist
                Map<String, Object> variables = Map.of("mediaId", anime.getAnilistId());
                JsonNode media = executeQuery(ONGOING_QUERY, variables)
                        .path("data").path("Media");

                int latestEpisodeCount = media.path("episodes").asInt(0);

                long existingEpisodes = episodeRepository.countBySeriesId(anime.getId());

                if (latestEpisodeCount > existingEpisodes) {
                    log.info("New episodes detected for {}: {} -> {}",
                            anime.getTitle(),
                            existingEpisodes,
                            latestEpisodeCount
                    );

                    //Create stubs for new episodes only
                    for (long i = existingEpisodes + 1; i <= latestEpisodeCount; i++) {
                        Episode episode = Episode.builder()
                                .series(anime)
                                .seasonNumber(1)
                                .episodeNumber((int) i)
                                .title("Episode " + i)
                                .subAvailable(false)
                                .dubAvailable(false)
                                .build();

                        episodeRepository.save(episode);

                        // Queue download for new episodes
                        TranscodeJob subJob = TranscodeJob.builder()
                                .media(anime)
                                .episode(episode)
                                .audioTrack(AudioTrack.SUB)
                                .status(TranscodeStatus.PENDING)
                                .build();

                        transcodeJobRepository.save(subJob);
                    }
                    //TODO: Notify users who watch this series
                }
                sleep();
            } catch (Exception e) {
                log.error("Failed to check episodes for {}: {}", anime.getTitle(), e.getMessage());
            }
        }
    }

    private MediaContent buildAndSaveMediaContent(JsonNode media) {

        Integer anilistId = media.path("id").asInt();

        //Skip if already exists
        Optional<MediaContent> existing = mediaRepository
                .findByAnilistId(anilistId);
        if (existing.isPresent()) {
            log.info("Anime {} already exists, skipping", anilistId);
            return existing.get();
        }

        //Parse titles
        String titleRomaji = media.path("title").path("romaji").asText();
        String titleEnglish = media.path("title").path("english").asText(null);
        String titleNative = media.path("title").path("native").asText(null);

        //Parse Genres
        List<String> genres = new ArrayList<>();
        media.path("genres").forEach(genre -> genres.add(genre.asText()));

        //Parse studio
        String studio = null;
        for (JsonNode s: media.path("studios").path("nodes")) {
            if (s.path("isAnimationStudio").asBoolean()) {
                studio = s.path("name").asText();
                break;
            }
        }

        //Parse airing status
        AiringStatus airingStatus = parseAiringStatus(
                media.path("status").asText()
        );

        //Parse release date
        LocalDate releaseDate = parseDate(media.path("startDate"));

        //Parse season
        String season = media.path("season").asText(null);
        int seasonYear =  media.path("seasonYear").asInt(0);

        //Parse rating
        double anilistScore = media.path("averageScore").asDouble(0);
        BigDecimal avgRating = BigDecimal.valueOf((anilistScore/100.0) * 5.0)
                .setScale(2, RoundingMode.HALF_UP);

        //Parse type
        int episodeCount = media.path("episodes").asInt(0);
        MediaType type = episodeCount == 1
                ? MediaType.MOVIE
                : MediaType.SERIES;

        MediaContent content = MediaContent.builder()
                .title(titleRomaji)
                .titleEnglish(titleEnglish)
                .titleJapanese(titleNative)
                .description(media.path("description").asText(null))
                .type(type)
                .genres(genres)
                .thumbnailUrl(media.path("coverImage").path("extraLarge").asText(null))
                .bannerUrl(media.path("bannerImage").asText(null))
                .anilistId(anilistId)
                .malId(media.path("idMal").asInt(0))
                .airingStatus(airingStatus)
                .totalEpisodes(episodeCount == 0 ? null : episodeCount)
                .season(season)
                .seasonYear(seasonYear == 0 ? null : seasonYear)
                .studio(studio)
                .releaseDate(releaseDate)
                .avgRating(avgRating)
                .hasSub(true)
                .hasDub(false)
                .build();

        mediaRepository.save(content);
        log.info("Saved anime: {}", content.getTitle());

        if (type == MediaType.SERIES && episodeCount > 0) {
            createEpisodeStubs(content, episodeCount);
        }

        //Trigger video download
        videoDownloadService.queueDownload(content);
        return content;
    }

    /*
    * Create Episode Stubs
    * Create episode records without video, yet
    * video gets filled in after download + transcode
    */
    private void createEpisodeStubs(MediaContent series, int episodeCount) {
        log.info("Creating {} episode stubs for {}", episodeCount, series.getTitle());

        for (int i = 1; i <= episodeCount; i++) {
            // Skip episode if it already exists
            boolean exists = episodeRepository.findBySeriesIdAndSeasonNumberAndEpisodeNumber(series.getId(), i, 1).isPresent();

            if (!exists) {
                Episode episode = Episode.builder()
                        .series(series)
                        .seasonNumber(1)
                        .episodeNumber(i)
                        .title("Episode " + i)
                        .subAvailable(false)
                        .dubAvailable(false)
                        .build();
                episodeRepository.save(episode);
            }
        }
    }

    // ------Execute GraphQL Query ---------------------------------
    private JsonNode executeQuery(String query, Map<String, Object> variables) {
        Map<String, Object> body = Map.of("query", query, "variables", variables);
        return webClient.post()
                .uri("/")
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    // -----Helpers --------------------------------------------------
    private AiringStatus parseAiringStatus(String status) {
        return switch (status.toUpperCase()) {
            case "RELEASING" -> AiringStatus.ONGOING;
            case "NOT_YET_RELEASED" -> AiringStatus.UPCOMING;
            case "HIATUS" -> AiringStatus.HIATUS;
            default -> AiringStatus.COMPLETED;
        };
    }

    private LocalDate parseDate(JsonNode dateNode) {
        try {
            int year = dateNode.path("year").asInt(0);
            int month = dateNode.path("month").asInt(1);
            int day = dateNode.path("day").asInt(1);
            if (year == 0) return null;
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private void sleep() {
        try {
            Thread.sleep((long) 700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
