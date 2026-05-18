package com.codewithpcodes.anistream.transcode;

import com.codewithpcodes.anistream.episode.Episode;
import com.codewithpcodes.anistream.media.MediaContent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "transcode_jobs")
public class TranscodeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AudioTrack audioTrack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TranscodeStatus status = TranscodeStatus.PENDING;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "raw_file_path", length = 512)
    private String rawFilePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
