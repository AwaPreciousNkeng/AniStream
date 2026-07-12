package com.codewithpcodes.anistream.media;

import aj.org.objectweb.asm.commons.Remapper;
import com.codewithpcodes.anistream.ranking.RankedMediaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<MediaContent, UUID> {

    

    @Query("select m from MediaContent m " +
            "where lower(m.title) like lower(concat('%', :query, '%')) " +
            "order by m.avgRating desc ")
    List<MediaContent> searchByTitle(
            @Param("query") String query
    );

    @Query(value = """
        SELECT DISTINCT m.* FROM media_content m
        INNER JOIN media_genres mg ON m.id = mg.media_id
        INNER JOIN user_genre_affinity uga ON mg.genre = uga.genre_name
        WHERE uga.user_id = :userId
        AND m.id NOT IN (
            SELECT wh.media_id FROM watch_history wh WHERE wh.user_id = :userId
        )
        ORDER BY (m.avg_rating * uga.weight) DESC
        LIMIT 20
        """, nativeQuery = true)
    List<MediaContent> getTopRecommendations(@Param("userId") UUID userId);

    // Spring Data fallback signature
    List<MediaContent> findTop20ByOrderByAvgRatingDesc();

    Optional<MediaContent> findByAnilistId(Integer anilistId);

    List<MediaContent> findByAiringStatus(AiringStatus airingStatus);

    @Query("""
    SELECT m FROM MediaContent m
    WHERE m.totalRatings >= :minRatings
    ORDER BY m.avgRating DESC
    """)
    List<MediaContent> findTopRatedWithMinimumRatings(
            @Param("minRatings") int minRatings,
            Pageable pageable);

    Page<MediaContent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
