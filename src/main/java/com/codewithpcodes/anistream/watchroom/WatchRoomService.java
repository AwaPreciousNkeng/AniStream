package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.chat.ChatRepository;
import com.codewithpcodes.anistream.media.MediaContent;
import com.codewithpcodes.anistream.media.MediaRepository;
import com.codewithpcodes.anistream.notification.NotificationService;
import com.codewithpcodes.anistream.notification.NotificationType;
import com.codewithpcodes.anistream.user.User;
import com.codewithpcodes.anistream.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
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
    private static final String INVITE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Transactional
    public WatchRoom createWatchRoom(UUID hostId, UUID mediaId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + hostId));
        MediaContent media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found with ID: " + mediaId));

        // Generate unique invite code
        String inviteCode = generateUniqueInviteCode();
    }

    public WatchRoom joinWatchRoom(String inviteCode, UUID userId) {
         WatchRoom watchRoom = watchRoomRepository
                 .findByInviteCode(inviteCode)
                 .orElseThrow(() -> new IllegalArgumentException("WatchRoom not found with Invite Code: " + inviteCode));

         // Validate joinability
        if (watchRoom.getStatus() == WatchRoomStatus.ENDED) {
            throw new IllegalStateException("WatchRoom is already ended");
        }

        if (watchRoom.getMaxParticipants() != null) {
            long count = watchRoomParticipantRepository.countByWatchRoomId(watchRoom.getId());
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
    public void inviteFriends(UUID watchRoomId, UUID hostId, List<UUID> friendIds) {
        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Watch room not found with ID: " + watchRoomId));

        User host = userRepository.findById(hostId)
                .orElseThrow(() ->  new IllegalArgumentException("User not found with ID: " + hostId));

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

    private void cacheWatchRoomState(UUID watchRoomID, WatchRoomStatus status) {
        redisTemplate.opsForValue().set(WATCH_ROOM_STATE_KEY + watchRoomID, status, 24, TimeUnit.HOURS);
    }

    private void cacheParticipant(UUID watchRoomId, UUID userId) {
        redisTemplate.opsForSet().add(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId + userId.toString());
        redisTemplate.expire(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, 24, TimeUnit.HOURS);
    }
    private void removeParticipantFromCache(UUID watchRoomId, UUID userId) {
        redisTemplate.opsForSet().remove(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, userId.toString());
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
