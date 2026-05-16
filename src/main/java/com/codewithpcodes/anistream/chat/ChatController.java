package com.codewithpcodes.anistream.chat;

import com.codewithpcodes.anistream.common.StringResponse;
import com.codewithpcodes.anistream.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Chats API")
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<StringResponse> createChat(
            @RequestParam(name = "sender-id")UUID senderId,
            @RequestParam(name = "receiver-id")UUID receiverId
    ) {
        final UUID chatId = chatService.createChat(senderId, receiverId);
        StringResponse stringResponse = StringResponse.builder().response(String.valueOf(chatId)).build();
        return ResponseEntity.ok(stringResponse);
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getChatByReceiver(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getChatsByReceiverId(currentUser));
    }
}
