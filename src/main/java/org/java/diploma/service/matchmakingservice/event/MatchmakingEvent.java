package org.java.diploma.service.matchmakingservice.event;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Setter
@Getter
public class MatchmakingEvent {
    private String type;       // "player_join", "player_leave", "queue_update"
    private Long userId;
    private Integer queueSize;
    private Instant timestamp;

}
