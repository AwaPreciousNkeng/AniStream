package com.codewithpcodes.anistream.watchroom;

import java.util.UUID;

public record ParticipantEvent(
        ParticipantEventType type,
        UUID userId,
        UUID watchRoomId,
        WatchRoomRole newRole
) {
}
