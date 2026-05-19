package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.chat.Chat;
import com.codewithpcodes.anistream.chat.ChatRepository;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.notification.NotificationService;
import com.codewithpcodes.anistream.notification.NotificationType;
import com.codewithpcodes.anistream.user.User;
import com.codewithpcodes.anistream.user.UserRepository;
import com.codewithpcodes.anistream.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class WatchRoomService {

    private final WatchRoomRepository watchRoomRepository;
    private final WatchRoomParticipantRepository watchRoomParticipantRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WATCH_ROOM_STATE_KEY = "watchroom:state:";
    private static final String WATCH_ROOM_PARTICIPANTS_KEY = "watchroom:participants:";


    // Invite code characters
    private static final String INVITE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Transactional
    public WatchRoom createWatchRoom(User host, UUID mediaId) {

        MediaContent media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found with ID: " + mediaId));

        // Generate unique invite code
        String inviteCode = generateUniqueInviteCode();
        Chat chat = Chat.builder()
    }

    public WatchRoom joinWatchRoom(String inviteCode, UUID userId) {

         WatchRoom watchRoom = watchRoomRepository
                 .findByInviteCode(inviteCode)
                 .orElseThrow(() -> new IllegalArgumentException("WatchRoom not found with Invite Code: " + inviteCode));

         // Validate joinability
        if (watchRoom.getStatus() == WatchRoomStatus.ENDED) {
            throw new IllegalStateException("WatchRoom is already ended");
        }

        // Check max participants
        if (watchRoom.getMaxParticipants() != null) {
            long count = watchRoomParticipantRepository
                    .countByWatchRoomId(watchRoom.getId());
            if (count >= watchRoom.getMaxParticipants()) {
                throw new IllegalStateException("Watch room is full");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // check is already joined
        boolean alreadyJoined = watchRoomParticipantRepository
                .existsByWatchRoomIdAndUserId(watchRoom.getId(), userId);

        if (!alreadyJoined) {
            watchRoomParticipantRepository.save(
                    WatchRoomParticipant.builder()
                            .watchRoom(watchRoom)
                            .user(user)
                            .role(WatchRoomRole.VIEWER)
                            .isConnected(true)
                            .lastKnownTimestamp(0.0)
                            .build()
            );

            if (!chatRepository)
        }
    }

    public List<WatchRoomParticipant> getParticipants(UUID watchRoomId) {
        return watchRoomParticipantRepository.findByWatchRoomId(watchRoomId);
    }

    @Transactional
    public void leaveWatchRoom(UUID watchRoomId, User user) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .ifPresent(p -> {
                    p.setIsConnected(false);
                    p.setLeftAt(LocalDateTime.now());
                    watchRoomParticipantRepository.save(p);
                });

        //Remove from redis member set
        redisTemplate.opsForSet().remove(
                WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, user.getId().toString()
        );

        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);

        // If host leaves, end the watch room
        if (watchRoom.getHost().getId().equals(user.getId())) {
            endWatchRoom(watchRoomId, user.getId());
            return;
        }

        //If no connected participants left, end the watch room
        long connected = watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream()
                .filter(WatchRoomParticipant::getIsConnected)
                .count();

        if (connected == 0) {
            endWatchRoom(watchRoomId, user.getId());
        }

        log.info("User {} left watch room {}", user.getUsername(), watchRoomId);
    }

    @Transactional
    public void endWatchRoom(UUID watchRoomId, UUID requesterId) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        // Only host can end the room
        boolean isHost = watchRoom.getHost().getId().equals(requesterId);

        boolean isAutoEnd = watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream().noneMatch(WatchRoomParticipant::getIsConnected);

        if (!isHost && !isAutoEnd) {
            throw new IllegalArgumentException("Only the host can end the room");
        }

        //Updated all participants to ONLINE
        watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .forEach(p -> {
                    User participant = p.getUser();
                    participant.setStatus(UserStatus.ONLINE);
                    userRepository.save(participant);
                    p.setIsConnected(false);
                    watchRoomParticipantRepository.save(p);
                });
        watchRoom.setStatus(WatchRoomStatus.ENDED);
        watchRoom.setEndedAt(LocalDateTime.now());
        watchRoomRepository.save(watchRoom);

        //clean up redis
        redisTemplate.delete(WATCH_ROOM_STATE_KEY + watchRoomId);
        redisTemplate.delete(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId);

        log.info("Watch room {} ended", watchRoomId);
    }

    @Transactional
    public void assignCoHost(UUID watchRoomId, UUID hostId, UUID targetUserId) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        if (!watchRoom.getHost().getId().equals(hostId)) {
            throw new IllegalArgumentException("Only the host can assign co-hosts");
        }

        watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream()
                .filter(p -> p.getUser().getId().equals(targetUserId))
                .findFirst()
                .ifPresent(p -> {
                    p.setRole(WatchRoomRole.CO_HOST);
                    watchRoomParticipantRepository.save(p);
                    log.info("User {} assigned as co-host in {}", targetUserId, watchRoomId);
                });
    }

    @Transactional
    public void toggleParticipantControl(UUID watchRoomId, UUID hostId) {
        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        if (!watchRoom.getHost().getId().equals(hostId)) {
            throw new IllegalArgumentException("Only the host can toggle participant control");
        }
        watchRoom.setAllowParticipantControl(!watchRoom.getAllowParticipantControl());

        watchRoomRepository.save(watchRoom);
        log.info("Participant control toggled to {} in {}",
                watchRoom.getAllowParticipantControl(),
                watchRoomId
        );
    }

    // Update playback state
    public void updateState(UUID watchRoomId, WatchRoomState state) {
        cacheWatchRoomState(watchRoomId, state);

        //Sync timestamp back to the DB periodically to survive Redis restarts
        watchRoomRepository.findById(watchRoomId)
                .ifPresent(w -> {
                    w.setCurrentTimestamp(state.getCurrentTimestamp());
                    w.setIsPlaying(state.isPlaying());
                    if (state.getAudioTrack() != null) {
                        w.setAudioTrack(WatchRoomAudioTrack.valueOf(state.getAudioTrack()));
                    }
                    if (state.getEpisodeId() != null) {
                        w.setCurrentEpisodeId(state.getEpisodeId());
                    }
                    watchRoomRepository.save(w);
                });
    }

    // Update participant's last known timestamp
    @Transactional
    public void updateParticipant(
            UUID watchRoomId,
            UUID userId,
            Double timestamp
    ) {
        watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(p -> {
                    p.setLastKnownTimestamp(timestamp);
                    watchRoomParticipantRepository.save(p);
                });
    }

    public WatchRoomState getWatchRoomState(UUID watchRoomId) {
        WatchRoomState state = (WatchRoomState) redisTemplate.opsForValue().get(WATCH_ROOM_STATE_KEY + watchRoomId);

        // Fallback to DB is redis miss
        if (state == null) {
            WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));
            state = buildState(watchRoom, watchRoom.getCurrentEpisodeId());
            cacheWatchRoomState(watchRoomId, state);
        }
    }

    @Transactional
    public void inviteFriends(UUID watchRoomId, User host, List<UUID> friendIds) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        // Send notification to each friend
        friendIds.forEach(friendId -> {
            User friend = userRepository.findById(friendId)
                    .orElse(null);

            if (friend != null) {
                notificationService.send(
                        friend,
                        host,
                        NotificationType.WATCH_HOME_INVITE,
                        Map.of(
                                "watchRoomId", watchRoomId.toString(),
                                "inviteCode", watchRoom.getInviteCode(),
                                "mediaTitle", watchRoom.getMedia().getTitle(),
                                "hostUsername", host.getUsername()
                        )
                );
            }
        });

        log.info("Invites sent for watch room: {}", watchRoomId);
    }

    private void addParticipant(WatchRoom watchRoom, User user) {
        WatchRoomParticipant participant = WatchRoomParticipant.builder()
                .watchRoom(watchRoom)
                .user(user)
                .build();
        watchRoomParticipantRepository.save(participant);
    }



    public boolean canControl(UUID watchRoomId, UUID userId) {
        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        if (watchRoom.getHost().getId().equals(userId)) {
            return true;
        }

        boolean isCoHost = watchRoomParticipantRepository.findByWatchRoomId(watchRoomId)
                .stream()
                .anyMatch(p ->
                        p.getUser().getId().equals(userId) &&
                        p.getRole() == WatchRoomRole.CO_HOST
                );
        if (isCoHost) return true;

        // Viewers can only if host allows it
        return watchRoom.getAllowParticipantControl();
    }

    private void cacheWatchRoomState(UUID watchRoomID, WatchRoomState state) {
        redisTemplate.opsForValue().set(WATCH_ROOM_STATE_KEY + watchRoomID, state, 24, TimeUnit.HOURS);
    }

    private void cacheParticipant(UUID watchRoomId, UUID userId) {
        redisTemplate.opsForSet().add(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId + userId.toString());
        redisTemplate.expire(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, 24, TimeUnit.HOURS);
    }
    private void removeParticipantFromCache(UUID watchRoomId, UUID userId) {
        redisTemplate.opsForSet().remove(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, userId.toString());
    }

    private WatchRoomState buildState(WatchRoom watchRoom, UUID episodeId) {
        return WatchRoomState.builder()
                .watchRoomId(watchRoom.getId())
                .episodeId(episodeId)
                .mediaId(watchRoom.getMedia().getId())
                .audioTrack(watchRoom.getAudioTrack().name())
                .isPlaying(watchRoom.getIsPlaying())
                .currentTimestamp(watchRoom.getCurrentTimestamp())
                .lastUpdatedAt(System.currentTimeMillis())
                .lastUpdatedBy(watchRoom.getHost().getId())
                .build();
    }

    private String generateUniqueInviteCode() {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(INVITE_CHARS.charAt(
                        random.nextInt(INVITE_CHARS.length())
                ));
            }
            code = sb.toString();
        } while (watchRoomRepository.existsByInviteCode(code));
        return code;
    }
}
