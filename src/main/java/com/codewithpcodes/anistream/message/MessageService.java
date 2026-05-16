package com.codewithpcodes.anistream.message;

import com.codewithpcodes.anistream.chat.Chat;
import com.codewithpcodes.anistream.chat.ChatRepository;
import com.codewithpcodes.anistream.file.FileService;
import com.codewithpcodes.anistream.file.FileUtils;
import com.codewithpcodes.anistream.notification.Notification;
import com.codewithpcodes.anistream.notification.NotificationService;
import com.codewithpcodes.anistream.notification.NotificationType;
import com.codewithpcodes.anistream.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final FileService fileService;
    private final NotificationService notificationService;

    public void saveMessage(MessageRequest request) {
        Chat chat = chatRepository.findById(request.chatId())
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        Message message = new Message();
        message.setContent(request.content());
        message.setChat(chat);
        message.setSenderId(request.senderId());
        message.setReceiverId(request.receiverId());
        message.setType(request.type());
        message.setState(MessageState.SENT);

        messageRepository.save(message);
        Notification notification = Notification.builder()
                .chatId(request.chatId())
                .messageType(request.type())
                .content(request.content())
                .senderId(request.senderId())
                .receiverId(request.receiverId())
                .notificationType(NotificationType.MESSAGE)
                .chatName(chat.getChatName(message.getSenderId()))
                .build();

        notificationService.sendNotification(message.getReceiverId(), notification);
    }

    public List<MessageResponse> findChatMessages(UUID chatId) {
        return messageRepository.findMessagesByChatId(chatId)
                .stream()
                .map(MessageResponse::fromMessage)
                .toList();
    }

    @Transactional
    public void setMessagesToSeen(UUID chatId, User currentUser) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID::" + chatId));
        final UUID recipientId = getRecipientId(chat, currentUser);
        messageRepository.setMessagesToSeenByChatId(chatId, MessageState.SEEN);
        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .notificationType(NotificationType.SEEN)
                .senderId(getSenderId(chat, currentUser))
                .receiverId(recipientId)
                .build();
        notificationService.sendNotification(recipientId, notification);
    }

    public void uploadMediaMessage(UUID chatId, MultipartFile file, User currentUser) throws IOException {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with ID::" + chatId));

        final UUID senderId = getSenderId(chat, currentUser);
        final UUID recipientId = getRecipientId(chat, currentUser);

        final String filePath = fileService.saveFile(file, senderId);
        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setReceiverId(recipientId);
        message.setType(MessageType.IMAGE);
        message.setState(MessageState.SENT);
        message.setMediaFilePath(filePath);
        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .notificationType(NotificationType.IMAGE)
                .messageType(MessageType.IMAGE)
                .senderId(senderId)
                .receiverId(recipientId)
                .media(FileUtils.readFileFromLocation(filePath))
                .build();

        notificationService.sendNotification(recipientId, notification);
    }

    private UUID getSenderId(Chat chat, User currentUser) {
        if (chat.getSender().getId().equals(currentUser.getId())) {
            return chat.getSender().getId();
        }
        return chat.getRecipient().getId();
    }

    private UUID getRecipientId(Chat chat, User currentUser) {
        if (chat.getSender().getId().equals(currentUser.getId())) {
            return chat.getRecipient().getId();
        }
        return chat.getSender().getId();
    }
}
