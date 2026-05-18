package com.codewithpcodes.anistream.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<MediaContent, UUID> {

    Page<MediaContent> findByGenreOrderByCreatedAtDesc(String genre, Pageable pageable);

    Page<MediaContent> findByTypeOrderByCreatedAtDesc(MediaType type, Pageable pageable);

    Page<MediaContent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<MediaContent> findAllByOrderByAvgRatingDesc(Pageable pageable);

    @Query("select m from MediaContent m " +
            "where lower(m.title) like lower(concat('%', :query, '%')) " +
            "order by m.avgRating desc ")
    List<MediaContent> searchByTitle(
            @Param("query") String query
    );

    @Query(value = "select distinct m.genre from MediaContent m " +
            "where m.genre is not null ")
    List<String> findAllGenres();
}
