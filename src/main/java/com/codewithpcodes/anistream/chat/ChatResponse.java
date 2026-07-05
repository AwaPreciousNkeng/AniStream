package com.codewithpcodes.anistream.chat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        String name,
        ChatType type,
        String avatarUrl,
        int unreadCount,
        int memberCount,
        String lastMessage,
        LocalDateTime lastMessageAt
) {
}
