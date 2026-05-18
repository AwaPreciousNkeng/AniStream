package com.codewithpcodes.anistream.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
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


}
