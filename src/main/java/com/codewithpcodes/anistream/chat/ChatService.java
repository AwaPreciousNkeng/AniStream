package com.codewithpcodes.anistream.chat;

import com.codewithpcodes.anistream.message.Message;
import com.codewithpcodes.anistream.message.MessageRepository;
import com.codewithpcodes.anistream.message.MessageType;
import com.codewithpcodes.anistream.notification.NotificationService;
import com.codewithpcodes.anistream.notification.NotificationType;
import com.codewithpcodes.anistream.storage.StorageService;
import com.codewithpcodes.anistream.user.User;
import com.codewithpcodes.anistream.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private StorageService storageService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_KEY = "chat:unread:";
    private static final String TYPING_KEY = "chat:typing:";
    private static final String ONLINE_KEY = "chat:online:";



    @Transactional
    public Chat createDM(UUID senderId, UUID receiverId) {

        // Check if DM already exists
        Optional<Chat> existingChat = chatRepository.findExistingDm(senderId, receiverId);

        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        User sender = findUser(senderId);

        User receiver = findUser(receiverId);

        Chat chat = new Chat();
        chat.setType(ChatType.DM);
        chat.setCreatedBy(sender);
        chatRepository.save(chat);

        //Add both members
        memberRepository.save(
                ChatMember.builder()
                        .chat(chat)
                        .user(sender)
                        .build()
        );

        log.info("Created DM between {} and {}", sender.getUsername(), receiver.getUsername());
        return chat;
    }

    @Transactional
    public Chat createGroupChat(UUID creatorId, String name, List<UUID> memberIds) {

        User creator = findUser(creatorId);

        Chat chat = new Chat();
        chat.setType(ChatType.GROUP);
        chat.setName(name);
        chat.setCreatedBy(creator);
        chatRepository.save(chat);

        memberRepository.save(
                ChatMember.builder()
                        .chat(chat)
                        .user(creator)
                        .build()
        );

        //Add all members
        memberIds.forEach(memberId -> {
            User member = userRepository
                    .findById(memberId)
                    .orElse(null);

            if (member != null) {
                memberRepository.save(
                        ChatMember.builder()
                                .chat(chat)
                                .user(member)
                                .build()
                );

                // Notify each member they were added
                notificationService.send(
                        member,
                        creator,
                        NotificationType.NEW_MESSAGE,
                        Map.of(
                                "chatId", chat.getId().toString(),
                                "group", name,
                                "message", creator.getUsername() + " added you to " + name
                        )
                );
            }
        });

        log.info("Group {} created by {}", name, creator.getUsername());
        return chat;
    }

    @Transactional
    public Message sendTextMessage(UUID senderId, String content, UUID chatId) {

        validateMember(chatId, senderId);

        Chat chat = findChat(chatId);

        User sender = findUser(senderId);

        // Sanitize content
        String sanitized = sanitize(content);

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(sanitized)
                .type(MessageType.TEXT)
                .build();

        messageRepository.save(message);
        incrementUnreadCounts(chatId, senderId);

        log.debug("Message sent in conversation {}", chatId);

        return message;
    }

    @Transactional
    public Message sendFileMessage(UUID chatId, UUID senderId, MultipartFile file) {

        validateMember(chatId, senderId);

        Chat chat = findChat(chatId);

        User sender = findUser(senderId);

        try {

            // Upload to Backblaze B2
            Path tempFile = Files.createTempFile("chat_", "_" + file.getOriginalFilename());

            file.transferTo(tempFile);

            String key = "chat/" + chatId
                    + "/" + UUID.randomUUID()
                    + "_" + file.getOriginalFilename();

            String fileUrl = storageService.uploadFile(tempFile, "animestream-avatars", key);

            Files.deleteIfExists(tempFile);

            Message message = Message.builder()
                    .chat(chat)
                    .sender(sender)
                    .content(file.getOriginalFilename())
                    .type(MessageType.FILE)
                    .metadata(Map.of(
                            "fileUrl", fileUrl,
                            "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                            "fileSize", file.getSize(),
                            "mimeType", Objects.requireNonNullElse(
                                    file.getContentType(),
                                    "application/octet-stream"
                            )
                    ))
                    .build();

            messageRepository.save(message);
            incrementUnreadCounts(chatId, senderId);

            return message;
        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Transactional
    public Message sendWatchRoomInvite(
            UUID chatId,
            UUID senderId,
            UUID watchRoomId,
            String inviteCode,
            String mediaTitle
    ) {

        validateMember(chatId, senderId);

        Chat chat = findChat(chatId);

        User sender = findUser(senderId);

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(sender.getUsername() + " invited you to watch " + mediaTitle)
                .type(MessageType.WATCH_ROOM_INVITE)
                .metadata(Map.of(
                        "watchRoomId", watchRoomId.toString(),
                        "inviteCode", inviteCode,
                        "mediaTitle", mediaTitle
                ))
                .build();

        messageRepository.save(message);
        incrementUnreadCounts(chatId, senderId);

        return message;
    }

    public Page<Message> getMessages(UUID chatId, UUID userId, int page, int size) {

        validateMember(chatId, userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());

        return messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable);
    }

    // TODO - paginate this later
    public List<ChatResponse> getMyChats(UUID userId) {

        List<Chat> chats = chatRepository.findAllByUserId(userId);

        return chats.stream()
                .map(chat -> buildChatResponse(chat, userId))
                .toList();
    }

    @Transactional
    public void addMemberToGroup(UUID chatId, UUID requesterId, UUID newMemberId) {

        Chat chat = findChat(chatId);

        // Only group chats
        if (chat.getType() != ChatType.GROUP) {
            throw new RuntimeException("Cannot add members to a DM");
        }

        // Validate requester is a member
        validateMember(chatId, requesterId);

        // Check if member already exists
        if (memberRepository.existsByChatIdAndUserId(chatId, newMemberId)) {
            return;
        }

        User newMember = findUser(newMemberId);
        User requester = findUser(requesterId);

        memberRepository.save(
                ChatMember.builder()
                        .chat(chat)
                        .user(newMember)
                        .build()
        );

        // Notify new members
        notificationService.send(
                newMember,
                requester,
                NotificationType.NEW_MESSAGE,
                Map.of(
                        "chatId", chatId.toString(),
                        "group", chat.getName(),
                        "message", requester.getUsername() + " added you to " + chat.getName()
                )
        );

        log.info("User {} added to group {}", newMember.getUsername(), chat.getName());
    }

    @Transactional
    public void removeMemberFromGroup(UUID chatId, UUID requesterId, UUID targetMemberId) {

        Chat chat = findChat(chatId);

        //Only creator can remove members
        if (!chat.getCreatedBy().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Only the group creator can remove members");
        }

        memberRepository.deleteByChatIdAndUserId(chatId, targetMemberId);

        log.info("User {} removed from group {}", targetMemberId, chatId);
    }



    public void markAsRead(UUID chatId, UUID userId) {

        //Reset the unread count in redis
        redisTemplate.opsForHash().put(UNREAD_KEY + userId, chatId.toString(), 0);
    }

    public int getUnreadContent(UUID chatId, UUID userId) {
        Object count = redisTemplate.opsForHash().get(
                UNREAD_KEY + chatId,
                chatId.toString()
        );
        return count == null ? 0 : (int) count;
    }

    public void setTyping(UUID chatId, UUID userId, boolean isTyping) {

        String key = TYPING_KEY + chatId;

        if (isTyping) {
            // TTL of 5s
            // Auto-expires if client disconnects
            redisTemplate.opsForSet().add(key, userId.toString());
            redisTemplate.expire(key, 5, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForSet().remove(key, userId.toString());
        }
    }

    public Set<Object> getTypingUsers(UUID chatId) {
        return redisTemplate.opsForSet().members(TYPING_KEY + chatId);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {

        Message message = messageRepository.findById(messageId)
                        .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Only sender can delete their message
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        messageRepository.delete(message);

        log.debug("Message {} delete by {}", messageId, userId);
    }

    @Transactional
    public void updateGroupName(UUID chatId, UUID requestId, String newName) {

        Chat chat = findChat(chatId);

        // Only the creator can rename
        if (!chat.getCreatedBy()
                .getId().equals(requestId)) {
            throw new RuntimeException("Only the group creator can rename the group");
        }

        chat.setName(newName);
        chatRepository.save(chat);
    }

    @Transactional
    public void updateGroupAvatar(UUID chatId, UUID requesterId, MultipartFile avatar) {

        Chat chat = findChat(chatId);

        if (!chat.getCreatedBy().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Only group creator can change the avatar");
        }
        // TODO - Change this in the future let any admin be able to also change things in the group like WhatsApp

        try {
            Path tempFile = Files.createTempFile("group_avatar_", ".jpg");
            avatar.transferTo(tempFile);

            String key = "group_avatars/" + chatId + ".jpg";

            String url = storageService.uploadFile(tempFile, "animestream-avatars", key);

            Files.deleteIfExists(tempFile);

            chat.setAvatarUrl(url);
            chatRepository.save(chat);

        } catch (Exception e) {
            throw new RuntimeException("Avatar upload failed", e);
        }
    }

    @Transactional
    public void leaveGroup(UUID chatId, UUID userId) {

        Chat chat = findChat(chatId);

        if (chat.getType() != ChatType.GROUP) {
            throw new IllegalArgumentException("Cannot leave a DM conversation");
        }

        memberRepository.deleteByChatIdAndUserId(chatId,  userId);

        log.info("User {} left the group {}", userId,  chatId);
    }

    private void validateMember(UUID chatId, UUID userId) {

        if (!chatRepository.isUserMember(chatId, userId)) {
            throw new IllegalArgumentException("You are not a member of this chat");
        }
    }

    private void incrementUnreadCounts(UUID chatId, UUID senderId) {

        List<ChatMember> members = memberRepository.findByChatId(chatId);

        members.stream()
                .filter(m -> !m.getUser()
                        .getId().equals(senderId))
                .forEach(m -> {
                    String key = UNREAD_KEY
                            + m.getUser().getId();
                    redisTemplate.opsForHash()
                            .increment(
                                    key,
                                    chatId.toString(),
                                    1
                            );
                });
    }

    private Chat findChat(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String sanitize(String content) {
        return content
                .replaceAll("<[^>]*>", "")
                .trim();
    }
    private ChatResponse buildChatResponse(Chat chat, UUID userId) {
        Page<Message> lastMessagePage = messageRepository
                .findByChatIdOrderBySentAtDesc(
                        chat.getId(),
                        PageRequest.of(0, 1)
                );

        Message lastMessage = lastMessagePage
                .hasContent()
                ? lastMessagePage.getContent().getFirst()
                : null;

        // Get unread message count from redis
        int unread = getUnreadContent(chat.getId(), userId);

        // Get members
        List<ChatMember> members = memberRepository.findByChatId(chat.getId());

        return new ChatResponse(
                chat.getId(),
                chat.getType() == ChatType.DM
                        ? members.stream()
                        .filter(m -> !m.getUser()
                                .getId().equals(userId))
                        .findFirst()
                        .map(m -> m.getUser().getFullName())
                        .orElse("Unknown")
                        : chat.getName(),
                chat.getType(),
                chat.getType() == ChatType.DM
                        ? members.stream()
                        .filter(m -> !m.getUser()
                                .getId().equals(userId))
                        .findFirst()
                        .map(m -> m.getUser().getAvatarUrl())
                        .orElse(null)
                        : chat.getAvatarUrl(),
                unread,
                members.size(),
                lastMessage != null
                        ? lastMessage.getContent()
                        : null,
                lastMessage != null
                        ? lastMessage.getSentAt()
                        : null
        );
    }
}

