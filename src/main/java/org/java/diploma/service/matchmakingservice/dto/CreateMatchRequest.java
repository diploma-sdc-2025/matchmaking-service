package org.java.diploma.service.matchmakingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMatchRequest {
    private Long player1Id;
    private Long player2Id;
    private Long matchmakingMatchId;
}