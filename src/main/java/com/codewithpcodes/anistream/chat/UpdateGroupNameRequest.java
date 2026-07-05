package com.codewithpcodes.anistream.chat;

import jakarta.validation.constraints.NotBlank;

public record UpdateGroupNameRequest(

        @NotBlank(message = "Group name is required")
        String name
) {
}
