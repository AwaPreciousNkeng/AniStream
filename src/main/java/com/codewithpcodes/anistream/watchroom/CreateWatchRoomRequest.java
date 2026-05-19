package com.codewithpcodes.anistream.watchroom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchRoomRequest {

    private UUID mediaId;

    private UUID episodeId;

    private WatchRoomAudioTrack audioTrack = WatchRoomAudioTrack.SUB;

    private boolean allowParticipantControl = false;

    private Integer maxParticipants;
}
