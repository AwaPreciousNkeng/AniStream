package com.codewithpcodes.anistream.message;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        String content,
        MessageType type,
        String senderAvatar,
        String senderUsername,
        Map<String, Object> metadata,
        UUID senderId,
        LocalDateTime sentAt
) {
}
