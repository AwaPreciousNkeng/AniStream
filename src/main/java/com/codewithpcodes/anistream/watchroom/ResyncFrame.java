package com.codewithpcodes.anistream.watchroom;

import java.util.UUID;

public record ResyncFrame(
        UUID watchRoomId,
        double correctTimestamp,
        boolean isPlaying
) {
}
