package com.codewithpcodes.anistream.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query(name = MessageConstants.FIND_MESSAGES_BY_CHAT_ID)
    List<Message> findMessagesByChatId(UUID chatId);

    @Query(name = MessageConstants.SET_MESSAGES_TO_SEEN_BY_CHAT)
    @Modifying
    void setMessagesToSeenByChatId(UUID chatId, MessageState newState);
}
