package org.java.diploma.service.matchmakingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GameServiceCreateMatchRequest(
        @NotEmpty List<Long> playerIds
) {}
