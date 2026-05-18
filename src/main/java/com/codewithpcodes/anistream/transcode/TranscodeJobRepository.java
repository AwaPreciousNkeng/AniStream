package com.codewithpcodes.anistream.transcode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {

    // Batch scheduler picks up next pending job
    Optional<TranscodeJob> findFirstByStatusOrderByCreatedAtAsc(TranscodeStatus status);

    //All jobs for media item
    List<TranscodeJob> findByMediaIdOrderByCreatedAtDesc(UUID mediaId);

    //All failed jobs
    List<TranscodeJob> finByStatusOrderByCreatedAtDesc(TranscodeStatus status);

    //Check if media already has a pending transcode job
    @Query(value = "select count(j) > 0 from TranscodeJob j " +
            "where j.media.id = :mediaId " +
            "and j.status in ('PENDING', 'PROCESSING')")
    boolean hasActiveJob(UUID mediaId);
}
