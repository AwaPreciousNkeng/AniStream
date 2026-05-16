package com.codewithpcodes.anistream.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        LocalDateTime lastSeen,
        boolean isOnline
) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getLastSeen(),
                user.isUserOnline()
        );
    }
}
