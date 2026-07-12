package com.codewithpcodes.anistream.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {
    // continue watching
    @Query(value = "select w from WatchHistory w " +
            "where w.user.id = :userId " +
            "and w.completed = false " +
            "and w.completionPercentage > 0 " +
            "order by w.lastWatchedAt desc")
    List<WatchHistory> findContinueWatching(
            @Param("userId") UUID userId
    );

    @Query(value = "select w.media.id from WatchHistory w where w.user.id = :userId")
    Set<UUID> findWatchedMediaIdsByUserId(@Param("userId") UUID userId);

    //Find specific history entry
    Optional<WatchHistory> findByUserIdAndMediaId(UUID userId, UUID mediaId);

    //Most watched genres for a user
    @Query(value = "select m.genres, count(w) as watchCount " +
            "from WatchHistory w " +
            "join w.media m " +
            "where w.user.id = :userId " +
            "and w.completionPercentage >= 70 " +
            "group by m.genres " +
            "order by watchCount desc")
    List<Object[]> findTopGenresByUser(
            @Param("userId") UUID userId
    );
}
