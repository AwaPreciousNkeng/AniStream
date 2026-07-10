package com.codewithpcodes.anistream.ranking;

import com.codewithpcodes.anistream.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
@Tag(name = "Rankings", description = "Anime ranking and trending lists")
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/top-rated")
    public ResponseEntity<List<RankedMediaResponse>> getTopRated(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(rankingService.getTopRated(Math.min(limit, 100)));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<RankedMediaResponse>> getTrending(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(rankingService.getTrending(Math.min(limit, 100)));
    }

    @GetMapping("/most-watched")
    public ResponseEntity<List<RankedMediaResponse>> getMostWatched(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(rankingService.getMostWatched(Math.min(limit, 100)));
    }

    @PostMapping("/view")
    public ResponseEntity<Void> recordView(
            @RequestParam UUID mediaId,
            @RequestParam UUID episodeId,
            @AuthenticationPrincipal User user
    ) {
        rankingService.recordView(mediaId, episodeId, user.getId());
        return ResponseEntity.ok().build();
    }
}
