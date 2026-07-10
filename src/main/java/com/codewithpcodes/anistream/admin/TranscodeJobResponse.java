package com.codewithpcodes.anistream.admin;

import com.codewithpcodes.anistream.transcode.AudioTrack;
import com.codewithpcodes.anistream.transcode.TranscodeJob;
import com.codewithpcodes.anistream.transcode.TranscodeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TranscodeJobResponse(
        UUID id,
        UUID mediaId,
        UUID episodeId,
        String audioTrack,
        TranscodeStatus status,
        String failureReason,
        int attemptCount,
        String rawFilePath,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TranscodeJobResponse from(TranscodeJob job) {
        return new TranscodeJobResponse(
                job.getId(),
                job.getMedia().getId(),
                job.getEpisode().getId(),
                job.getAudioTrack().name(),
                job.getStatus(),
                job.getFailureReason(),
                job.getAttemptCount(),
                job.getRawFilePath(),
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
