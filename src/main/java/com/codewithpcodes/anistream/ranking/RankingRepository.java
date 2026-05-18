package com.codewithpcodes.anistream.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RankingRepository extends JpaRepository<Ranking, UUID> {
     // Get top N for a period
    List<Ranking> findByPeriodOrderByRankAsc(RankingPeriod period);

    //Get top N for a period starting from a data
    List<Ranking> findByPeriodAndPeriodStartOrderByRankAsc(RankingPeriod period, Long periodStart);

    //Clea old rankings before recomputing
    @Modifying
    @Query(value = "delete from Ranking r " +
            "where r.period = :period " +
            "and r.periodStart = :periodStart")
    void deleteByPeriodAndPeriodStart(
            @Param("period") RankingPeriod period,
            @Param("periodStart") Long periodStart
    );
}
