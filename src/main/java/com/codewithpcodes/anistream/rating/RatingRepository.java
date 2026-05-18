package com.codewithpcodes.anistream.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Page<Rating> findByMediaIdOrderByCreatedAtDesc(UUID mediaId, Pageable pageable);

    Page<Rating> findByEpisodeIdOrderByCreatedAtDesc(UUID episodeId, Pageable pageable);

    Optional<Rating> findByUserIdAndMediaId(UUID userId, UUID mediaId);

    @Query(" select avg(r.stars) from Rating r " +
            "where r.media.id = :mediaId")
    Double computedAvgRating(
            @Param("mediaId") UUID mediaId
    );

    long countByMediaId(UUID mediaId);
}
