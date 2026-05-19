package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "watch_room_participants")
@IdClass(WatchParticipationId.class)
public class WatchRoomParticipant {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watch_room_id", nullable = false)
    private WatchRoom watchRoom;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WatchRoomRole role = WatchRoomRole.VIEWER;

    @Column(name = "is_connected", nullable = false)
    @Builder.Default
    private Boolean isConnected = true;

    @Column(name = "last_known_timestamp")
    @Builder.Default
    private Double lastKnownTimestamp = 0.0;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;
}
