package org.java.diploma.service.matchmakingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueueStatusResponse {
    private boolean inQueue;
    private Integer position;
    private Integer queueSize;
    /** Game-service match id when the user was recently paired (not in queue anymore). */
    private Long matchId;
}
