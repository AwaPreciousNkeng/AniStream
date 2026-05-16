package com.codewithpcodes.anistream.message;

import java.util.UUID;

public record MessageRequest(
        String content,
        UUID senderId,
        UUID receiverId,
        MessageType type,
        UUID chatId
) {
}
