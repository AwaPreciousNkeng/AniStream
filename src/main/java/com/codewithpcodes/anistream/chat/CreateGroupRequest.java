package com.codewithpcodes.anistream.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(

        @NotBlank(message = "Group name is required")
        String name,

        @NotEmpty(message = "At least one user is required")
        List<UUID> userIds
) {
}
