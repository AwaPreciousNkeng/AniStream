package com.codewithpcodes.anistream.message;

import java.util.UUID;

public record TypingEvent(
        UUID userId,
        boolean isTyping
) {
}
