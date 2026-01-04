package org.java.diploma.service.matchmakingservice.dto;

import java.time.Instant;

public record QueueJoinResponse(
        String status,
        Long userId,
        Long queueSize,
        Instant joinedAt
) {}

