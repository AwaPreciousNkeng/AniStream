package com.codewithpcodes.anistream.message;

import com.codewithpcodes.anistream.file.FileUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        String content,
        MessageType type,
        MessageState state,
        UUID senderId,
        UUID receiverId,
        LocalDateTime createdAt,
        byte[] media
) {
    public static MessageResponse fromMessage(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getType(),
                message.getState(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getCreatedDate(),
                FileUtils.readFileFromLocation(message.getMediaFilePath())
        );
    }
}
