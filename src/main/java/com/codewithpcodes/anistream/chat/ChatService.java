package com.codewithpcodes.anistream.chat;

import com.codewithpcodes.anistream.user.User;
import com.codewithpcodes.anistream.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatResponse> getChatsByReceiverId(User currentUser) {
        final UUID userId = currentUser.getId();
        return chatRepository.findChatsBySenderId(userId)
                .stream()
                .map(c -> ChatResponse.fromChat(c, userId))
                .toList();
    }
    public UUID createChat(UUID senderId, UUID receiverId) {
        Optional<Chat> existingChat = chatRepository.findChatByReceiverAndSender(senderId, receiverId);
        if (existingChat.isPresent()) {
            return existingChat.get().getId();
        }
        User sender = userRepository.findByPublicId(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found with ID::" + senderId));
        User receiver = userRepository.findByPublicId(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found with ID::" + receiverId));

        Chat chat = new Chat();
        chat.setSender(sender);
        chat.setRecipient(receiver);
        return chatRepository.save(chat).getId();
    }
}
