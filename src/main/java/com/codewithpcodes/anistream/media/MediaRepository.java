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
