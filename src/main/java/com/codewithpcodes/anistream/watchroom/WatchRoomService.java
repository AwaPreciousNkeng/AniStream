package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.chat.*;
import com.codewithpcodes.anistream.exceptions.BadRequestException;
import com.codewithpcodes.anistream.exceptions.ForbiddenException;
import com.codewithpcodes.anistream.exceptions.ResourceNotFoundException;
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
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WATCH_ROOM_STATE_KEY = "watchroom:state:";
    private static final String WATCH_ROOM_PARTICIPANTS_KEY = "watchroom:participants:";


    // Invite code characters
    private static final String INVITE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Transactional
    public WatchRoomResponse createWatchRoom(
            User host,
            CreateWatchRoomRequest request
    ) {

        MediaContent media = mediaRepository.findById(request.getMediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Media not found"));

        // Generate unique invite code
        String inviteCode = generateUniqueInviteCode();
        Chat chat = Chat.builder()
                .type(ChatType.GROUP)
                .name(media.getTitle() + "- Watch Room")
                .createdBy(host)
                .build();
        chatRepository.save(chat);

        //Add host to chat
        chatMemberRepository.save(
                ChatMember.builder()
                        .chat(chat)
                        .user(host)
                        .build()
        );

        //Build watch room
        WatchRoom watchRoom = WatchRoom.builder()
                .host(host)
                .media(media)
                .chat(chat)
                .inviteCode(inviteCode)
                .status(WatchRoomStatus.WAITING)
                .audioTrack(WatchRoomAudioTrack.SUB)
                .currentTimestamp(0.0)
                .isPlaying(false)
                .allowParticipantControl(
                        request.isAllowParticipantControl()
                )
                .maxParticipants(request.getMaxParticipants())
                .build();
        watchRoomRepository.save(watchRoom);

        watchRoomParticipantRepository.save(
                WatchRoomParticipant.builder()
                        .watchRoom(watchRoom)
                        .user(host)
                        .role(WatchRoomRole.HOST)
                        .isConnected(true)
                        .lastKnownTimestamp(0.0)
                        .build()
        );

        host.setStatus(UserStatus.IN_WATCH_ROOM);
        userRepository.save(host);

        //Cache initial state in redis
        cacheWatchRoomState(watchRoom.getId(), buildState(watchRoom, null));

        log.info("Watch room created: {} | host: {} | media: {}",
                watchRoom.getId(),
                host.getUsername(),
                media.getTitle()
        );
        return WatchRoomResponse.toWatchResponse(watchRoom);
    }

    public WatchRoomResponse joinWatchRoom(String inviteCode, UUID userId) {

         WatchRoom watchRoom = watchRoomRepository
                 .findByInviteCode(inviteCode)
                 .orElseThrow(() -> new ResourceNotFoundException("WatchRoom not found"));

        if (watchRoom.getStatus() == WatchRoomStatus.ENDED) {
            throw new BadRequestException("WatchRoom is already ended");
        }

        // Check max participants
        if (watchRoom.getMaxParticipants() != null) {
            long count = watchRoomParticipantRepository
                    .countByWatchRoomId(watchRoom.getId());
            if (count >= watchRoom.getMaxParticipants()) {
                throw new BadRequestException("Watch room is full");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // check if already joined
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

            if (!chatMemberRepository
                    .existsByChatIdAndUserId(watchRoom.getChat().getId(), user.getId())) {
                chatMemberRepository.save(
                        ChatMember.builder()
                                .chat(watchRoom.getChat())
                                .user(user)
                                .build()
                );
            }

            //Update user presence
            user.setStatus(UserStatus.IN_WATCH_ROOM);
            userRepository.save(user);

            //Cache participant
            cacheParticipant(watchRoom.getId(), user.getId());

            //Activate if watch room is more than 1 participant
            long count = watchRoomParticipantRepository.countByWatchRoomId(watchRoom.getId());

            if (count > 1 &&
                    watchRoom.getStatus() == WatchRoomStatus.WAITING) {
                watchRoom.setStatus(WatchRoomStatus.ACTIVE);
                watchRoomRepository.save(watchRoom);
            }
            log.info("User {} joined watch Room {}", user.getUsername(), watchRoom.getId());
        } else {
            watchRoomParticipantRepository
                    .findByWatchRoomId(watchRoom.getId())
                    .stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setIsConnected(true);
                        watchRoomParticipantRepository.save(p);
                    });
        }
        return WatchRoomResponse.toWatchResponse(watchRoom);
    }

    public List<ParticipantResponse> getParticipants(UUID watchRoomId, User requester) {

        boolean isParticipant = watchRoomParticipantRepository
                .existsByWatchRoomIdAndUserId(watchRoomId, requester.getId());

        if (!isParticipant) {
            throw new ForbiddenException("Forbidden - only participants can view watch room participants");
        }

        return watchRoomParticipantRepository.findByWatchRoomId(watchRoomId)
                .stream()
                .map(ParticipantResponse::toParticipantResponse)
                .toList();
    }

    @Transactional
    public void leaveWatchRoom(UUID watchRoomId, User user) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

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
        removeParticipantFromCache(watchRoomId, user.getId());

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
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

        // Only host can end the room
        boolean isHost = watchRoom.getHost().getId().equals(requesterId);

        boolean isAutoEnd = watchRoomParticipantRepository
                .findByWatchRoomId(watchRoomId)
                .stream().noneMatch(WatchRoomParticipant::getIsConnected);

        if (!isHost && !isAutoEnd) {
            throw new ForbiddenException("Only the host can end the room");
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
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

        if (!watchRoom.getHost().getId().equals(hostId)) {
            throw new ForbiddenException("Only the host can assign co-hosts");
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
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

        if (!watchRoom.getHost().getId().equals(hostId)) {
            throw new ForbiddenException("Only the host can toggle participant control");
        }
        watchRoom.setAllowParticipantControl(!watchRoom.getAllowParticipantControl());

        watchRoomRepository.save(watchRoom);
        log.info("Participant control toggled to {} in {}",
                watchRoom.getAllowParticipantControl(),
                watchRoomId
        );
    }

    public WatchRoomResponse getWatchRoom(UUID watchRoomId, User requester) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

        //Verify if the requester is a participant
        boolean isParticipant = watchRoomParticipantRepository
                .existsByWatchRoomIdAndUserId(watchRoomId, requester.getId());

        if (!isParticipant) {
            throw new ForbiddenException("Forbidden - only participant can view watch room details");
        }

        return WatchRoomResponse.toWatchResponse(watchRoom);
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
    public void updateParticipantTimestamp(
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
                    .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));
            state = buildState(watchRoom, watchRoom.getCurrentEpisodeId());
            cacheWatchRoomState(watchRoomId, state);
        }
        return state;
    }

    @Transactional
    public void inviteFriends(UUID watchRoomId, User host, List<UUID> friendIds) {

        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

        // Send notification to each friend
        friendIds.forEach(friendId -> userRepository.findById(friendId).ifPresent(friend -> notificationService.send(
                friend,
                host,
                NotificationType.WATCH_HOME_INVITE,
                Map.of(
                        "watchRoomId", watchRoomId.toString(),
                        "inviteCode", watchRoom.getInviteCode(),
                        "mediaTitle", watchRoom.getMedia().getTitle(),
                        "hostUsername", host.getUsername()
                )
        )));

        log.info("Invites sent for watch room: {}", watchRoomId);
    }


    public List<WatchRoomResponse> getActiveWatchRooms(User requester) {
        return watchRoomRepository.findActiveByUserId(requester.getId())
                .stream()
                .map(WatchRoomResponse::toWatchResponse)
                .toList();
    }


    public boolean canControl(UUID watchRoomId, UUID userId) {
        WatchRoom watchRoom = watchRoomRepository.findById(watchRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Watch room not found"));

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
        redisTemplate.opsForSet().add(WATCH_ROOM_PARTICIPANTS_KEY + watchRoomId, userId.toString());
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
