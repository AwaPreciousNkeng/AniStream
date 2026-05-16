package com.codewithpcodes.anistream.auth;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AuthenticationResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        UUID userID,
        String fullName,
        String username
) {
    public static AuthenticationResponse fromAuth(
            String accessToken,
            String refreshToken,
            UUID userID,
            String fullName,
            String username
    ) {
        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                userID,
                fullName,
                username
        );
    }
}
