package com.codewithpcodes.anistream.friendship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    @Query(value = "select f from Friendship f " +
            "where (f.requester.id = :userId1 and f.addressee.id = :userId2 ) " +
            "or (f.requester.id = :userId2 and f.addressee.id = :userId1) ")
    Optional<Friendship> findBetweenUsers(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2
    );

    List<Friendship> findAllByAddresseeIdAndStatus(UUID userId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatus(UUID userId, FriendshipStatus status);
}
