package com.codewithpcodes.anistream.watchroom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerControlFrame {

    private UUID senderId;

    private PlayerAction action;

    private double timestamp;

    private UUID episodeId;

    private String audioTrack;
}
