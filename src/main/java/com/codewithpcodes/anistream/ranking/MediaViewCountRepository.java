package com.codewithpcodes.anistream.ranking;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaViewCountRepository extends JpaRepository<MediaViewCount, UUID> {

    // find today's record for a media item
    Optional<MediaViewCount> findByMediaIdAndViewDate(UUID mediaId, LocalDate viewDate);

    //Top viewed media in a date range
    // Used by ranking scheduler to compute
    // daily / weekly / monthly top 10
    @Query(value = "select v.media.id, sum(v.viewCount) as total " +
            "from MediaViewCount v " +
            "where v.viewDate between :from and :to " +
            "order by total desc " +
            "limit :limit")
    List<Object[]> findTopMediaInRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("limit") int limit
    );

    @Query("""
    SELECT v.media, SUM(v.viewCount) as totalViews
    FROM MediaViewCount v
    GROUP BY v.media
    ORDER BY totalViews DESC
    """)
    List<Object[]> findMostWatchedAllTime(Pageable pageable);

    @Modifying
    @Query(value = """
    INSERT INTO media_view_counts (id, media_id, episode_id, user_id, view_count, view_date)
    VALUES (gen_random_uuid(), :mediaId, :episodeId, :userId, 1, NOW())
    ON CONFLICT (media_id, episode_id, user_id, DATE(view_date))
    DO UPDATE SET view_count = media_view_counts.view_count + 1
    """, nativeQuery = true)
    void incrementViewCount(
            @Param("mediaId") UUID mediaId,
            @Param("episodeId") UUID episodeId,
            @Param("userId") UUID userId);
}
