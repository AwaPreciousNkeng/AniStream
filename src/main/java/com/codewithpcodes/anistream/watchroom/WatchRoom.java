package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.chat.Chat;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "watch_rooms")
public class WatchRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaContent media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Column(name = "invite_code", unique = true, nullable = false)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WatchRoomStatus status = WatchRoomStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_track",  nullable = false)
    @Builder.Default
    private WatchRoomAudioTrack audioTrack = WatchRoomAudioTrack.SUB;

    @Column(name = "current_episode_id")
    private UUID currentEpisodeId;

    @Column(name = "current_timestamp")
    @Builder.Default
    private Double currentTimestamp = 0.0;

    @Column(name = "is_playing", nullable = false)
    @Builder.Default
    private Boolean isPlaying = false;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "allow_participant_control", nullable = false)
    @Builder.Default
    private Boolean allowParticipantControl = false;

    @OneToMany(mappedBy = "watchRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WatchRoomParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
