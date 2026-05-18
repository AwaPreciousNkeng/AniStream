package com.codewithpcodes.anistream.crawler;

import com.codewithpcodes.anistream.episode.Episode;
import com.codewithpcodes.anistream.episode.EpisodeRepository;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaType;
import com.codewithpcodes.anistream.transcode.AudioTrack;
import com.codewithpcodes.anistream.transcode.TranscodeJob;
import com.codewithpcodes.anistream.transcode.TranscodeJobRepository;
import com.codewithpcodes.anistream.transcode.TranscodeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoDownloadService {

    private final EpisodeRepository episodeRepository;
    private final TranscodeJobRepository transcodeJobRepository;

    @Value("${application.ytdlp.binary-path}")
    private String ytdlpPath;

    @Value("${application.ytdlp.download-dir}")
    private String downloadDir;


    public void queueDownload(MediaContent media) {
        if (media.getType() == MediaType.MOVIE) {
            queueMovieDownload(media);
        } else {
            queueSeriesDownload(media);
        }
    }

    private void queueSeriesDownload(MediaContent media) {
        List<Episode> episodes = episodeRepository
                .findBySeriesIdOrderBySeasonNumberAscEpisodeNumberAsc(media.getId());

        for (Episode episode : episodes) {
            TranscodeJob subJob = TranscodeJob.builder()
                    .media(media)
                    .episode(episode)
                    .audioTrack(AudioTrack.SUB)
                    .status(TranscodeStatus.PENDING)
                    .build();
            transcodeJobRepository.save(subJob);

            TranscodeJob dubJob = TranscodeJob.builder()
                    .media(media)
                    .episode(episode)
                    .audioTrack(AudioTrack.DUB)
                    .status(TranscodeStatus.PENDING)
                    .build();
            transcodeJobRepository.save(dubJob);
        }

        log.info("Queued {} episode download jobs for series: {}", episodes.size() * 2, media.getTitle());
    }

    public void queueMovieDownload(MediaContent media) {
        TranscodeJob subJob = TranscodeJob.builder()
                .media(media)
                .audioTrack(AudioTrack.SUB)
                .status(TranscodeStatus.PENDING)
                .build();
        transcodeJobRepository.save(subJob);

        TranscodeJob dubJob = TranscodeJob.builder()
                .media(media)
                .audioTrack(AudioTrack.DUB)
                .status(TranscodeStatus.PENDING)
                .build();
        transcodeJobRepository.save(dubJob);

        log.info("Queued download jobs for movie: {}", media.getTitle());
    }

    @Async("transcodingExecutor")
    public String downloadEpisode(MediaContent media, Episode episode, AudioTrack audioTrack) {
        // Build search query for yt-dlp
        String searchQuery = buildSearchQuery(media, episode, audioTrack);

        String outputDir = downloadDir + "/" + media.getId();
        String outputFile = outputDir + "/" +
                (episode != null
                        ? "ep" + episode.getEpisodeNumber()
                          + "_" + audioTrack.name().toLowerCase()
                        : audioTrack.name().toLowerCase()
                ) + ".%(ext)s";

        try {
            Path dir = Paths.get(outputDir);
            Files.createDirectories(dir);

            List<String> cmd = List.of(
                    ytdlpPath,
                    "--format", "bestvideo_bestaudio/best",
                    "--merge-output-format", "mp4",
                    "--output", outputFile,
                    "--no-playlist",
                    "--retries", "3",
                    "--fragment-retries", "3",
                    "ytsearch1:" + searchQuery
            );

            log.info("Starting download: {}", searchQuery);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("yt-dlp: {}", line);
                }
            }
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("yt-dlp exited with code " + exitCode);
            }

            //Find the downloaded file
            Path downloaded = Files.list(dir)
                    .filter(p -> p.toString().contains(
                            episode != null
                                    ? "ep" + episode.getEpisodeNumber()
                                      + "_" + audioTrack.name().toLowerCase()
                                    : audioTrack.name().toLowerCase()
                    ))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Downloaded file not found"));
            log.info("Download complete: {}", downloaded);
            return downloaded.toString();
        } catch (Exception e) {
            log.error("Download failed for {}: {}", searchQuery, e.getMessage());
            throw new RuntimeException("Download failed", e);
        }
    }

    private String buildSearchQuery(MediaContent media, Episode episode, AudioTrack audioTrack) {
        StringBuilder query = new StringBuilder();
        query.append(media.getTitle());
        if (episode != null) {
            query.append(" Episode ")
                    .append(episode.getEpisodeNumber());
        }
        if (audioTrack == AudioTrack.DUB) {
            query.append(" English Dub ");
        } else {
            query.append(" English Sub ");
        }

        return query.toString();
    }

}
