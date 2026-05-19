package com.codewithpcodes.anistream.watchroom;

import com.codewithpcodes.anistream.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchroom")
@RequiredArgsConstructor
public class WatchRoomController {

    private final WatchRoomService watchRoomService;
    private final WatchRoomRepository watchRoomRepository;
    private final WatchRoomParticipantRepository watchRoomParticipantRepository;

    @PostMapping("/create")
    public ResponseEntity<WatchRoomResponse> create(
            @RequestBody CreateWatchRoomRequest request,
            @AuthenticationPrincipal User host
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(watchRoomService.createWatchRoom(host, request));
    }

    @PostMapping("/join/{invite-code}")
    public ResponseEntity<WatchRoomResponse> join(
            @PathVariable("invite-code") String inviteCode,
            @AuthenticationPrincipal User host
    ) {
        return ResponseEntity.ok(watchRoomService.joinWatchRoom(inviteCode, host.getId()));
    }

    @PostMapping("/{watch-room-id}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User host
    ) {
        watchRoomService.leaveWatchRoom(watchRoomId, host);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{watch-room-id}/end")
    public ResponseEntity<Void> end(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User host
    ) {
        watchRoomService.endWatchRoom(watchRoomId, host.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{watch-room-id}/invite")
    public ResponseEntity<Void> invite(
            @PathVariable("watch-room-id") UUID watchRoomID,
            @RequestBody List<UUID> friendIds,
            @AuthenticationPrincipal User host
    ) {
        watchRoomService.inviteFriends(watchRoomID, host,  friendIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{watch-room-id}/cohost/{target-user-id}")
    public ResponseEntity<Void> assignCoHost(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @PathVariable("target-user-id") UUID targetUserId,
            @AuthenticationPrincipal User host
    ) {
        watchRoomService.assignCoHost(watchRoomId, targetUserId, host.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{watch-room-id}/toggle-control")
    public ResponseEntity<Void> toggleControl(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User host
    ) {
        watchRoomService.toggleParticipantControl(watchRoomId, host.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{watch-room-id}")
    public ResponseEntity<WatchRoomResponse> getWatchRoom(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(watchRoomService.getWatchRoom(watchRoomId, requester));
    }

    @GetMapping("/{watch-room-id}/state")
    public ResponseEntity<WatchRoomState> getWatchRoomState(
            @PathVariable("watch-room-id") UUID watchRoomId
    ) {
        return ResponseEntity.ok(watchRoomService.getWatchRoomState(watchRoomId));
    }

    @GetMapping("/{watch-room-id}/partipants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(watchRoomService.getParticipants(watchRoomId, requester));
    }

    @GetMapping("/my/active")
    public ResponseEntity<List<WatchRoomResponse>> getActiveWatchRooms(
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(watchRoomService.getActiveWatchRooms(requester));
    }

    @GetMapping("/{watch-room-id}/can-control")
    public ResponseEntity<Boolean> canControl(
            @PathVariable("watch-room-id") UUID watchRoomId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(watchRoomService.canControl(watchRoomId, requester.getId()));
    }

}
