package com.codewithpcodes.anistream.watchroom;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParticipantResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        WatchRoomRole role,
        boolean isConnected,
        double lastKnownTimestamp,
        LocalDateTime joinedAt
) {
    public static ParticipantResponse toParticipantResponse(WatchRoomParticipant participant) {
        return new ParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getUsername(),
                participant.getUser().getFullName(),
                participant.getUser().getAvatarUrl(),
                participant.getRole(),
                participant.getIsConnected(),
                participant.getLastKnownTimestamp(),
                participant.getJoinedAt()
        );
    }
}
