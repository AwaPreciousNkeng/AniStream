package com.codewithpcodes.anistream.recommendation;

import com.codewithpcodes.anistream.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalised anime recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                recommendationService.getRecommendations(user.getId(), Math.min(limit, 50))
        );
    }
}
