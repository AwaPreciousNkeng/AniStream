package com.codewithpcodes.anistream.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(UUID userId, Notification notification) {
        log.info("Sending notification to user {} with payload {}", userId,  notification);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/chat",
                notification
        );
    }
}
