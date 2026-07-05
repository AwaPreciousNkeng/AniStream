package com.codewithpcodes.anistream.message;

import java.util.UUID;

public record ReadEvent(
        UUID userId,
        UUID chatId
) {
}
