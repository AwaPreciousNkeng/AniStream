package com.codewithpcodes.anistream.chat;

import java.util.UUID;

public record ChatResponse(
        UUID id,
        String name,
        long unreadCount,
        String lastMessage,
        boolean isRecipientOnline,
        UUID senderId,
        UUID receiverId
) {
    public static ChatResponse fromChat(Chat chat, UUID senderId) {
        return new ChatResponse(
                chat.getId(),
                chat.getChatName(senderId),
                chat.getUnreadMessagesCount(senderId),
                chat.getLastMessage(),
                chat.getRecipient().isUserOnline(),
                chat.getSender().getId(),
                chat.getRecipient().getId()
        );
    }
}
