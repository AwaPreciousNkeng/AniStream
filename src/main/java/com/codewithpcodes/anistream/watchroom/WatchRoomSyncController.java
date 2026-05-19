package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WatchRoomSyncController {

    private final WatchRoomService watchRoomService;
    private final WatchRoomParticipantRepository watchRoomParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/watch-room/{watch-room-id}/control")
    public void handleControl(
            @DestinationVariable("watch-room-id") String watchRoomId,
            @Payload PlayerControlFrame frame,
            @AuthenticationPrincipal User user
    ) {

        UUID watchRoomID = UUID.fromString(watchRoomId);

        // validate if sender has control permission
        if (!watchRoomService.canControl(watchRoomID, user.getId())) {
            //send an error back to the sender only
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/errors",
                    new SyncError(
                            "NO_PERMISSION",
                            "You do not have permission to control playback"
                    )
            );
            return;
        }

        WatchRoomState state = watchRoomService.getWatchRoomState(watchRoomID);

        applyAction(state, frame, user.getId());

        watchRoomService.updateState(watchRoomID, state);

        // Broadcast to all participants
        messagingTemplate.convertAndSend(
                "/topic/watch-room/" + watchRoomId + "/sync",
                state
        );

        log.debug("Sync event {} broadcast -> watch room: {}", frame.getAction(), watchRoomId);
    }

    @MessageMapping("/watch-room/{watch-room-id}/sync-request")
    public void handleSyncRequest(
            @DestinationVariable String watchRoomId,
            @AuthenticationPrincipal User user,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID watchRoomID = UUID.fromString(watchRoomId);

        boolean isParticipant = watchRoomParticipantRepository
                .existsByWatchRoomIdAndUserId(watchRoomID, user.getId());

        if (!isParticipant) {
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/errors",
                    new SyncError(
                            "NOT_A_PARTICIPANT",
                            "You are not a participant of this watch room"
                    )
            );
            return;
        }

        // Save room metadata to session attributes for clean disconnect handling later
        if (headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put("watchRoomId", watchRoomID);
        }

        WatchRoomState state = watchRoomService.getWatchRoomState(watchRoomID);

        // send state directly to the requesting user only
        // So they jump to the right timestamp immediately
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/watch-room/sync",
                state
        );

        // Notify Others that someone joined
        messagingTemplate.convertAndSend(
                "/topic/watch-room/" + watchRoomId + "/participants",
                new ParticipantEvent(
                        ParticipantEventType.JOINED,
                        user.getId(),
                        watchRoomID,
                        null
                )
        );

        log.debug("Sync state sent to new participant: {}", user.getUsername());
    }

    @MessageMapping("/watch-room/{watch-room-id}/heartbeat")
    public void handleHeartbeat(
            @DestinationVariable String watchRoomId,
            @Payload HeartbeatFrame frame,
            @AuthenticationPrincipal User user
    ) {
        UUID watchRoomID = UUID.fromString(watchRoomId);

        watchRoomService.updateParticipantTimestamp(watchRoomID, user.getId(), frame.timestamp());

        WatchRoomState state = watchRoomService.getWatchRoomState(watchRoomID);

        //calculate drift
        double drift = Math.abs(frame.timestamp() - state.getCurrentTimestamp());

        //if drift exceeds 1.5 seconds - force resync
        if (drift > 1.5) {
            log.debug("Drift detected for user {}: {}s", user.getUsername(), drift);

            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/watch-room/resync",
                    new ResyncFrame(
                            watchRoomID,
                            state.getCurrentTimestamp(),
                            state.isPlaying()
                    )
            );
        }
    }

    @MessageMapping("/watch-room/{watch-room-id}/role-change")
    public void handleRoleChange(
            @DestinationVariable String watchRoomId,
            @Payload RoleChangeFrame frame,
            @AuthenticationPrincipal User user
    ) {
        UUID watchRoomID = UUID.fromString(watchRoomId);

        watchRoomService.assignCoHost(watchRoomID, user.getId(), frame.targetUserId());

        // Notify all participants about the role change
        messagingTemplate.convertAndSend(
                "/topic/watch-room/" + watchRoomId + "/participants",
                new ParticipantEvent(
                        ParticipantEventType.ROLE_CHANGED,
                        frame.targetUserId(),
                        watchRoomID,
                        WatchRoomRole.CO_HOST
                )
        );

        log.debug("User {} assigned as co-host in {}", frame.targetUserId(), watchRoomID);
    }

    private void applyAction(WatchRoomState state, PlayerControlFrame frame, UUID userId) {
        switch (frame.getAction()) {
            case PLAY -> {
                state.setPlaying(true);
                state.setCurrentTimestamp(frame.getTimestamp());
            }

            case PAUSE -> {
                state.setPlaying(false);
                state.setCurrentTimestamp(frame.getTimestamp());
            }

            case SEEK -> {
                state.setCurrentTimestamp(frame.getTimestamp());
            }

            case EPISODE_CHANGE -> {
                state.setEpisodeId(frame.getEpisodeId());
                state.setPlaying(false);
                state.setCurrentTimestamp(0.0);
            }

            case TRACK_CHANGE -> {
                state.setAudioTrack(frame.getAudioTrack());
                state.setCurrentTimestamp(frame.getTimestamp());
            }

            default -> log.warn("Unknown player action: {}", frame.getAction());
        }

        state.setLastUpdatedBy(userId);
        state.setLastUpdatedAt(System.currentTimeMillis());
    }
}
