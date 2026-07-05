package com.codewithpcodes.anistream.message;

import java.util.UUID;

public record DeleteMessageFrame(
        UUID messageId
) {
}
