package com.codewithpcodes.anistream.request;

import com.codewithpcodes.anistream.transcode.AudioTrack;
import com.codewithpcodes.anistream.user.User;
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
@Table(name = "anime_requests")
public class AnimeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "anime_title", nullable = false)
    private String animeTitle;

    @Column(name = "anilist_id")
    private Integer anilistId;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_track", nullable = false)
    private AudioTrack preferredTrack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "vote_count")
    @Builder.Default
    private Integer voteCount = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
