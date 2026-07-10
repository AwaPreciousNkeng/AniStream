package com.codewithpcodes.anistream.admin;

import com.codewithpcodes.anistream.crawler.AnilistMetadataCrawler;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.transcode.TranscodeJob;
import com.codewithpcodes.anistream.transcode.TranscodeJobRepository;
import com.codewithpcodes.anistream.transcode.TranscodeStatus;
import com.codewithpcodes.anistream.transcode.TranscodingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin-only operations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AnilistMetadataCrawler crawler;
    private final TranscodingService transcodingService;
    private final TranscodeJobRepository transcodeJobRepository;
    private final MediaRepository mediaRepository;

    @PostMapping("/crawler/trigger")
    public ResponseEntity<String> triggerCrawl(
            @RequestParam(defaultValue = "1") int page
    ) {
        log.info("Admin triggered Anilist crawl for page {}", page);
        Thread.ofVirtual().start(() -> crawler.fetchAndSaveTrending(page));
        return ResponseEntity.accepted().body("Crawl triggered for page " + page);
    }

    @PostMapping("/crawler/ongoing")
    public ResponseEntity<String> triggerOngoingCheck() {
        log.info("Admin triggered ongoing anime episode check");
        Thread.ofVirtual().start(crawler::checkOngoingAnimeForNewEpisodes);
        return ResponseEntity.accepted().body("Ongoing episode check triggered");
    }

    @GetMapping("/transcode-jobs")
    public ResponseEntity<Page<TranscodeJobResponse>> getTranscodeJobs(
            @RequestParam(required = false) TranscodeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TranscodeJob> jobs = status != null
                ? transcodeJobRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size))
                : transcodeJobRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(jobs.map(TranscodeJobResponse::from));
    }

    @DeleteMapping("/transcode-jobs/failed")
    public ResponseEntity<String> clearFailedJobs() {
        int deleted = transcodeJobRepository.deleteFailedJobsOlderThan7Days();
        return ResponseEntity.ok("Deleted " + deleted + " failed transcode jobs");
    }

    @GetMapping("/media")
    public ResponseEntity<Page<AdminMediaResponse>> getAllMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                mediaRepository
                        .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                        .map(AdminMediaResponse::from)
        );
    }

    @DeleteMapping("/media/{media-id}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable("media-id") UUID mediaId
    ) {
        mediaRepository.deleteById(mediaId);
        log.warn("Admin deleted media {}", mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(new AdminStatsResponse(
                mediaRepository.count(),
                transcodeJobRepository.countByStatus(TranscodeStatus.PENDING),
                transcodeJobRepository.countByStatus(TranscodeStatus.PROCESSING),
                transcodeJobRepository.countByStatus(TranscodeStatus.FAILED)
        ));
    }
}
