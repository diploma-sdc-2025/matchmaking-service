package org.java.diploma.service.matchmakingservice.client;

import org.java.diploma.service.matchmakingservice.dto.CreateMatchRequest;
import org.java.diploma.service.matchmakingservice.dto.GameServiceCreateMatchRequest;
import org.java.diploma.service.matchmakingservice.dto.GameServiceMatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class GameServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceClient.class);

    private static final String CREATE_MATCH_ENDPOINT = "/api/game/matches";
    private static final String GAME_SERVICE_REQUEST =
            "Sending match creation request to Game Service for players {} and {}";
    private static final String GAME_SERVICE_SUCCESS =
            "Successfully created match in Game Service: gameServiceMatchId={}";
    private static final String GAME_SERVICE_FAILURE =
            "Failed to create match in Game Service: {}";
    private static final String GAME_SERVICE_INITIALIZED =
            "GameServiceClient initialized with URL: {}";
    private static final String EMPTY_RESPONSE_ERROR =
            "Empty response from Game Service";
    private static final String MATCH_CREATION_FAILED =
            "Failed to create match in Game Service";

    private final RestTemplate restTemplate;
    private final String gameServiceUrl;

    public GameServiceClient(
            RestTemplate restTemplate,
            @Value("${game.service.url}") String gameServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.gameServiceUrl = gameServiceUrl;
        logger.info(GAME_SERVICE_INITIALIZED, gameServiceUrl);
    }

    public GameServiceMatchResponse createMatch(CreateMatchRequest request) {
        Long p1 = request != null ? request.getPlayer1Id() : null;
        Long p2 = request != null ? request.getPlayer2Id() : null;

        logger.info(GAME_SERVICE_REQUEST, p1, p2);

        try {
            GameServiceCreateMatchRequest body =
                    new GameServiceCreateMatchRequest(List.of(p1, p2));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GameServiceCreateMatchRequest> entity =
                    new HttpEntity<>(body, headers);

            String url = gameServiceUrl + CREATE_MATCH_ENDPOINT;

            ResponseEntity<GameServiceMatchResponse> response =
                    restTemplate.postForEntity(url, entity, GameServiceMatchResponse.class);

            GameServiceMatchResponse matchResponse = response.getBody();
            if (matchResponse != null) {
                logger.info(GAME_SERVICE_SUCCESS, matchResponse.getMatchId());
                return matchResponse;
            }

            throw new RuntimeException(EMPTY_RESPONSE_ERROR);

        } catch (Exception ex) {
            logger.error(GAME_SERVICE_FAILURE, ex.getMessage(), ex);
            throw new RuntimeException(MATCH_CREATION_FAILED, ex);
        }
    }
}
