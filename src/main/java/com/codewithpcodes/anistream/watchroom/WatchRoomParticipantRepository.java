package com.codewithpcodes.anistream.watchroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WatchRoomParticipantRepository extends JpaRepository<WatchRoomParticipant, WatchParticipationId> {
    List<WatchRoomParticipant> findByWatchRoomId(UUID watchRoomId);

    boolean existsByWatchRoomIdAndUserId(UUID watchRoomId, UUID userId);

    void deleteByWatchRoomIdAndUserId(UUID watchRoomId, UUID userId);

    long countByWatchRoomId(UUID watchRoomId);
}
