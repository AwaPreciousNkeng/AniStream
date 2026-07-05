package com.codewithpcodes.anistream.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query(value = "select c from Chat c " +
            "join c.members m " +
            "where m.user.id = :userId " +
            "order by c.createdAt desc")
    List<Chat> findAllByUserId(@Param("userId") UUID userId);

    @Query(value = "select c from Chat c " +
            "join c.members m1 " +
            "join c.members m2 " +
            "where c.type = 'DM' " +
            "and m1.user.id = :userId1 " +
            "and m2.user.id = :userId2")
    Optional<Chat> findExistingDm(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2
    );

    //check if a user is a member of a chat
    @Query(value = "select count(m) > 0 " +
            "from ChatMember m " +
            "where m.chat.id = :chatId " +
            "and m.user.id = :userId")
    boolean isUserMember(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId
    );
}
