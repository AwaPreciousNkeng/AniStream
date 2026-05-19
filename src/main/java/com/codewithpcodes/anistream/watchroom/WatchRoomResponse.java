package com.codewithpcodes.anistream.watchroom;

import java.time.LocalDateTime;
import java.util.UUID;

public record WatchRoomResponse(
        UUID id,
        String inviteCode,
        WatchRoomStatus status,
        WatchRoomAudioTrack audioTrack,
        boolean isPlaying,
        double currentTimestamp,
        UUID currentEpisodeId,
        boolean allowParticipantControl,
        Integer maxParticipants,

        //Media Info
        UUID mediaId,
        String mediaTitle,
        String mediaThumbnailUrl,

        //Host info
        UUID hostId,
        String hostUsername,

        //Chat for chat sidebar
        UUID chatId,

        long participantCount,
        LocalDateTime createdAt,
        LocalDateTime endedAt
) {

    public static WatchRoomResponse toWatchResponse(WatchRoom watchRoom) {
        return new WatchRoomResponse(
                watchRoom.getId(),
                watchRoom.getInviteCode(),
                watchRoom.getStatus(),
                watchRoom.getAudioTrack(),
                watchRoom.getIsPlaying(),
                watchRoom.getCurrentTimestamp(),
                watchRoom.getCurrentEpisodeId(),
                watchRoom.getAllowParticipantControl(),
                watchRoom.getMaxParticipants(),
                watchRoom.getMedia().getId(),
                watchRoom.getMedia().getTitle(),
                watchRoom.getMedia().getThumbnailUrl(),
                watchRoom.getHost().getId(),
                watchRoom.getHost().getUsername(),
                watchRoom.getChat().getId(),
                watchRoom.getParticipants().stream().filter(WatchRoomParticipant::getIsConnected).count(),
                watchRoom.getCreatedAt(),
                watchRoom.getEndedAt()
        );
    }
}
