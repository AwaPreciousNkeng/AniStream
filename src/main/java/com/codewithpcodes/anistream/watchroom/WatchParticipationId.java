package com.codewithpcodes.anistream.watchroom;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class WatchParticipationId implements Serializable {
    private UUID watchRoom;
    private UUID user;
}
