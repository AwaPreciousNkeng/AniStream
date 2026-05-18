package com.codewithpcodes.anistream.crawler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/crawler")
@RequiredArgsConstructor
public class CrawlerController {

    private final AnilistMetadataCrawler metadataCrawler;

    @PostMapping("/fetch/id/{anilistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchById(
            @PathVariable("anilistId") Integer anilistId
    ) {
        var result = metadataCrawler.fetchAndSaveByAnilistId(anilistId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fetch/title")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchByTitle(
            @RequestBody String title
    ) {
        var result = metadataCrawler.fetchAndSaveByTitle(title);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fetch/trending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchByTrending(
            @RequestParam(defaultValue = "3") int pages
    ) {
        var results = metadataCrawler.fetchAndSaveTrending(pages);
        return ResponseEntity.ok(results.size() + " anime fetched");
    }
}
