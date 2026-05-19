package com.codewithpcodes.anistream.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(name = MessageConstants.FIND_MESSAGES_BY_CHAT_ID)
    Page<Message> findMessagesByChatId(UUID chatId, Pageable pageable);

    @Query(name = MessageConstants.SET_MESSAGES_TO_SEEN_BY_CHAT)
    @Modifying
    void setMessagesToSeenByChatId(UUID chatId, MessageState newState);

    //Count unread messages in a chat for a user
    @Query(value = "select  count(m) from Message m " +
            "where m.chat.id = :chatId " +
            "and m.senderId != :userId " +
            "and m.createdAt > (" +
            "select coalesce(max(m2.createdAt), '1970-01-01') " +
            "from Message m2 " +
            "where m2.chat.id = :chatId " +
            "and m2.senderId = :userId)")
    long countUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );
}
