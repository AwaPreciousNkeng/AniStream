package com.codewithpcodes.anistream.watchroom;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchRoomState implements Serializable {

    private UUID watchRoomId;

    private UUID mediaId;

    private UUID episodeId;

    private String audioTrack;

    private boolean isPlaying;

    private double currentTimestamp;

    private UUID lastUpdatedBy;

    private long lastUpdatedAt;
}
