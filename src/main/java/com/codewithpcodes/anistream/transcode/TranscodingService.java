package com.codewithpcodes.anistream.transcode;

import com.codewithpcodes.anistream.crawler.VideoDownloadService;
import com.codewithpcodes.anistream.episode.Episode;
import com.codewithpcodes.anistream.episode.EpisodeRepository;
import com.codewithpcodes.anistream.exceptions.TranscodingException;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscodingService {
    
    private final TranscodeJobRepository transcodeJobRepository;
    private final EpisodeRepository episodeRepository;
    private final MediaRepository mediaRepository;
    private final VideoDownloadService videoDownloadService;
    private final StorageService storageService;

    @Value("${application.ffmpeg.binary-path}")
    private String ffmpegPath;
    
    @Value("${application.ffmpeg.output-dir}")
    private String outputDir;
    
    @Retryable(
            retryFor = { TranscodingException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000, multiplier = 2.0)
    ) 
    @Transactional
    public void processJob(TranscodeJob job) {
        log.info("Processing transcode job: {} | {} | {}",
                job.getId(),
                job.getMedia().getTitle(),
                job.getAudioTrack()
        );
        
        //Mark as processing
        job.setStatus(TranscodeStatus.PROCESSING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        transcodeJobRepository.save(job);
        
        try {
            // Step 1 - Download the video
            String rawFilePath = videoDownloadService.downloadEpisode(
                    job.getMedia(),
                    job.getEpisode(),
                    job.getAudioTrack()
            );
            job.setRawFilePath(rawFilePath);
            transcodeJobRepository.save(job);
            
            // Step 2 - Transcode via FFmpeg
            String transcodeFolder = transcodeToHls(job, rawFilePath);

            // Step 3 - Upload to Backblaze B2
            String s3Prefix = buildS3Prefix(job);
            storageService.uploadTranscodedFolder(transcodeFolder, s3Prefix);

            // Step 4 - Build master playlist URL
            String masterUrl = storageService.buildPublicUrl("animestream-playlists", s3Prefix + "/master.m3u8");

            // Step 5 - Update episode or media with playlist URL
            updatePlaylistUrl(job, masterUrl);

            // Step 6 - Clean up local temp files
            cleanUpLocalFiles(rawFilePath, transcodeFolder);

            // Step 7 - Mark job as done
            job.setStatus(TranscodeStatus.DONE);
            job.setCompletedAt(LocalDateTime.now());
            transcodeJobRepository.save(job);

            log.info("Transcode job complete: {} | {}",
                    job.getMedia().getTitle(),
                    job.getAudioTrack()
            );
        } catch (Exception e) {
            throw new TranscodingException("Transcode failed: " + e.getMessage(), e);
        }
    }

    private String transcodeToHls(TranscodeJob job, String inputPath) throws Exception {
        String jobFolder = outputDir + job.getId();
        Files.createDirectories(Paths.get(jobFolder));

        // Transcode all 3 quality variants
        transcodeVariant(inputPath, jobFolder, "1080p", "1920:1080", "4500k");
        transcodeVariant(inputPath, jobFolder, "720p", "1280:720", "2500k");
        transcodeVariant(inputPath, jobFolder, "480p", "854:480", "1000k");

        generateMasterPlaylist(jobFolder);

        return jobFolder;
    }

    private void transcodeVariant(
            String inputPath,
            String outputFolder,
            String label,
            String scale,
            String bitrate
    ) throws Exception {
        String variantDir = outputFolder + "/" + label;
        Files.createDirectories(Paths.get(variantDir));

        List<String> cmd = List.of(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + scale,
                "-c:v", "libx264",
                "-b:v", bitrate,
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "4",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", variantDir + "/segment_%03d.ts",
                variantDir + "/playlist.m3u8",
                "-y"
        );
        log.info("Transcoding {} variant...", label);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // stream FFmpeg logs
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                log.debug("ffmpeg [{}]: {}", label, line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new TranscodingException("FFmpeg failed for variant " + label
                    + " with exit code: " + exitCode
            );
        }
        log.info("variant {} transcoded successfully.", label);
    }

    private void generateMasterPlaylist(String outputFolder) throws IOException {
        String masterContent = """
                #EXTM3U
                #EXT-X-VERSION: 3
                
                #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080
                1080p/playlist.m3u8
                
                #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
                720p/playlist.m3u8
                
                #EXT-X-STREAM-INF:BANDWIDTH=1000000,RESOLUTION=854x480
                480p/playlist.m3u8
                """;

        Path masterPath = Paths.get(outputFolder + "/master.m3u8");
        Files.writeString(masterPath, masterContent);

        log.info("Master playlist generated: {}", masterPath);
    }

    private void updatePlaylistUrl(TranscodeJob job, String masterUrl) {
        AudioTrack track = job.getAudioTrack();
        Episode episode = job.getEpisode();

        if (episode != null) {
            if (track == AudioTrack.SUB) {
                episode.setMasterPlaylistUrlSub(masterUrl);
                episode.setSubAvailable(true);
            } else {
                episode.setMasterPlaylistUrlDub(masterUrl);
                episode.setDubAvailable(true);
            }
            episodeRepository.save(episode);

            if (track == AudioTrack.DUB) {
                MediaContent series = job.getMedia();
                series.setHasDub(true);
                mediaRepository.save(series);
            }
        } else {
            MediaContent movie = job.getMedia();
            if (track == AudioTrack.SUB) {
                movie.setMasterPlaylistUrlSub(masterUrl);
                movie.setHasSub(true);
            } else {
                movie.setMasterPlaylistUrlDub(masterUrl);
                movie.setHasDub(true);
            }
            mediaRepository.save(movie);
        }
        log.info("Playlist URL updated: {}", masterUrl);
    }

    private String buildS3Prefix(TranscodeJob job) {
        StringBuilder prefix = new StringBuilder();
        prefix.append("anime/")
                .append(job.getMedia().getId());

        if (job.getEpisode() != null) {
            prefix.append("/s")
                    .append(job.getEpisode().getSeasonNumber())
                    .append("/ep")
                    .append(job.getEpisode().getEpisodeNumber());
        }

        prefix.append("/")
                .append(job.getAudioTrack().name().toLowerCase());

        return prefix.toString();
    }

    private void cleanUpLocalFiles(String rawFilePath, String transcodeFolder) {
        try {
            Path rawPath = Paths.get(rawFilePath);
            Files.deleteIfExists(rawPath);
        } catch (IOException e) {
            log.warn("Could not delete raw file {}", rawFilePath, e);
        }

        Path folder =  Paths.get(transcodeFolder);

        if (Files.exists(folder)) {
            try (Stream<Path> pathStream = Files.walk(folder)) {
                pathStream.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("Could not delete file inside transcode folder {}", p, e);
                            }
                        });
                log.info("Successfully cleaned up transcode folder {}", transcodeFolder);
            } catch (IOException e) {
                log.warn("Failed during directory walk of {}", transcodeFolder, e);
            }
        }
    }

    @Recover
    @Transactional
    public void recoverFromFailure(TranscodingException ex, TranscodeJob job) {
        log.error("Transcode job permanently failed after 3 attempts: {} | {}",
                job.getMedia().getTitle(),
                job.getAudioTrack(),
                ex
        );
        job.setStatus(TranscodeStatus.FAILED);
        job.setFailureReason(ex.getMessage());
        transcodeJobRepository.save(job);

        //TODO - send admin alert (email/webhook)
    }
}
