package com.codewithpcodes.anistream.transcode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {

    // Batch scheduler picks up next pending job
    Optional<TranscodeJob> findFirstByStatusOrderByCreatedAtAsc(TranscodeStatus status);

    Page<TranscodeJob> findByStatusOrderByCreatedAtDesc(TranscodeStatus status, Pageable pageable);

    Page<TranscodeJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(TranscodeStatus status);

    @Modifying
    @Query(value = """
    DELETE FROM transcode_jobs
    WHERE status = 'FAILED'
    AND created_at < NOW() - INTERVAL '7 days'
    """, nativeQuery = true)
    int deleteFailedJobsOlderThan7Days();

    //Check if media already has a pending transcode job
    @Query(value = "select count(j) > 0 from TranscodeJob j " +
            "where j.media.id = :mediaId " +
            "and j.status in ('PENDING', 'PROCESSING')")
    boolean hasActiveJob(UUID mediaId);
}
