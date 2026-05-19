package com.codewithpcodes.anistream.watchroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchRoomRepository extends JpaRepository<WatchRoom, UUID> {

    //Join by invite code
    Optional<WatchRoom> findByInviteCode(String inviteCode);

    //All active watch rooms for a user
    @Query(value = "select w from WatchRoom w " +
            "join w.participants p " +
            "where p.user.id = :userId " +
            "and w.status = 'ACTIVE'")
    List<WatchRoom> findActiveByUserId(
            @Param("userId") UUID userId
    );

    //All watch rooms hosted by a user
    List<WatchRoom> findByHostIdOrderByCreatedAtDesc(UUID hostId);

    //check if invite code exists
    boolean existsByInviteCode(String inviteCode);
}
