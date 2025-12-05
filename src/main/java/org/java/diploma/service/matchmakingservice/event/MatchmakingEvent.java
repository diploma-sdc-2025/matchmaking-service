package org.java.diploma.service.matchmakingservice.event;

import lombok.Data;

import java.time.Instant;

@Data
public class MatchmakingEvent {
    private String type;       // "player_join", "player_leave", "queue_update"
    private Long userId;
    private Integer queueSize;
    private Instant timestamp;
}
