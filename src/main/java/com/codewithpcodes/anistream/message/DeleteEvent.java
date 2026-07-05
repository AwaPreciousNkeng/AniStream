package com.codewithpcodes.anistream.message;

import java.util.UUID;

public record DeleteEvent(
        UUID messageId,
        UUID deletedBy
) {
}
