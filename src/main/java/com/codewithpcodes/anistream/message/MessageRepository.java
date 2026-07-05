package com.codewithpcodes.anistream.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Paginated message history for a chat
    Page<Message> findByChatIdOrderBySentAtDesc(UUID chatId, Pageable pageable);

    //Count unread messages in a chat for a user
    @Query(value = "select  count(m) " +
            "from Message m " +
            "where m.chat.id = :chatId " +
            "and m.sender.id != :userId " +
            "and m.sentAt > (" +
            "select coalesce(max(m2.sentAt), '1970-01-01') " +
            "from Message m2 " +
            "where m2.chat.id = :chatId " +
            "and m2.sender.id = :userId)")
    long countUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );
}
