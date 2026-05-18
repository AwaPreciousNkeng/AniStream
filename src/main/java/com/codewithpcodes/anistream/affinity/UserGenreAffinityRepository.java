package com.codewithpcodes.anistream.affinity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGenreAffinityRepository extends JpaRepository<UserGenreAffinity, UUID> {
    List<UserGenreAffinity> findByUserIdOrderByAffinityScoreDesc(UUID userId);

    Optional<UserGenreAffinity> findByUserIdAndGenre(UUID userId, String genre);

    //Top genres for a user
    @Query("select a from UserGenreAffinity a " +
            "where a.user.id = :userId " +
            "order by a.affinityScore desc " +
            "limit 5")
    List<UserGenreAffinity> findTopGenres(
            @Param("userId") UUID userId
    );
}
