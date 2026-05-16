package com.codewithpcodes.anistream.notification;

import com.codewithpcodes.anistream.message.MessageType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    private UUID chatId;
    private String content;
    private UUID senderId;
    private UUID receiverId;
    private String chatName;
    private MessageType messageType;
    private NotificationType notificationType;
    private byte[] media;
}
