package com.codewithpcodes.anistream.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimeRequestRepository extends JpaRepository<AnimeRequest, UUID> {

    List<AnimeRequest> findByStatusOrderByVoteCountDesc(RequestStatus status);

    Optional<AnimeRequest> findByAnimeTitleIgnoreCase(String animeTitle);

    Optional<AnimeRequest> findByAnilistId(Integer anilistId);

    Page<AnimeRequest> findByRequestedByIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
