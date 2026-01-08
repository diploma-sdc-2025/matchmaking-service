package org.java.diploma.service.matchmakingservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GameServiceMatchResponse {
    private Long matchId;
    private String status;
    private Integer currentRound;
    private List<Long> playerIds;
}
