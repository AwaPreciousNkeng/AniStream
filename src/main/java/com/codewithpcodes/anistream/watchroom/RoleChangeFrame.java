package com.codewithpcodes.anistream.watchroom;

import java.util.UUID;

public record RoleChangeFrame(
        UUID targetUserId
) {
}
