package com.codewithpcodes.anistream.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ChatMemberId implements Serializable {
    private UUID chat;
    private UUID user;
}
