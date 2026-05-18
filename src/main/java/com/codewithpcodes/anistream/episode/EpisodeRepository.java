package com.codewithpcodes.anistream.episode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    //Get all episodes by series ordered
    List<Episode> findBySeriesIdOrderBySeasonNumberAscEpisodeNumberAsc(UUID seriesId);

    //Get a specific episode
    Optional<Episode> findBySeriesIdAndSeasonNumberAndEpisodeNumber(
            UUID seriesId,
            Integer seasonNumber,
            Integer episodeNumber
    );

    //Get all seasons for a series
    @Query(value = "select distinct e.seasonNumber from Episode e " +
            "where e.series.id = :seriesId " +
            "order by e.seasonNumber asc")
    List<Integer> findSeasonsBySeriesId(
            @Param("seriesId") UUID seriesId
    );

    //Get latest episode for a series
    @Query(value = "select e from Episode e " +
            "where e.series.id = :seriesId " +
            "order by e.seasonNumber desc, e.episodeNumber desc " +
            "limit 1")
    Optional<Episode> findLatestEpisode(
            @Param("seriesId") UUID seriesId
    );

    long countBySeriesId(UUID id);

}
