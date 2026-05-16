package com.codewithpcodes.anistream.message;

import com.codewithpcodes.anistream.user.User;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Messages API")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<Void> saveMessage(@RequestBody MessageRequest request) {
        messageService.saveMessage(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping(value = "/{chat-id}/upload-media", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadMedia(
            @PathVariable("chat-id") UUID chatId,
            @Parameter()
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) throws IOException {
        messageService.uploadMediaMessage(chatId, file, currentUser);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @PatchMapping("/{chat-id}")
    public ResponseEntity<Void> setMessagesToSeen(
            @PathVariable("chat-id") UUID chatId,
            @AuthenticationPrincipal User currentUser
    ) {
        messageService.setMessagesToSeen(chatId, currentUser);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @GetMapping("/chat/{chat-id}")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable("chat-id") UUID chatId
    ) {
        return ResponseEntity
                .ok(messageService.findChatMessages(chatId));
    }
}
