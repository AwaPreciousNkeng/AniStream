package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WatchRoomDisconnectListener {

    private final WatchRoomService watchRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 1. Get the authenticated user from the session attributes
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return;
        }

        // 2. Track which room they were in (best stored in session attributes when they join/sync)
        UUID watchRoomId = (UUID) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("watchRoomId");

        if (watchRoomId != null) {
            watchRoomService.leaveWatchRoom(watchRoomId, user);

            // Notify remaining participants
            messagingTemplate.convertAndSend(
                    "/topic/watch-room/" + watchRoomId + "/participants",
                    new ParticipantEvent(
                            ParticipantEventType.LEFT,
                            user.getId(),
                            watchRoomId,
                            null
                    )
            );
            log.info("User {} cleanly removed from room {}", user.getUsername(), watchRoomId);
        }
    }
}
