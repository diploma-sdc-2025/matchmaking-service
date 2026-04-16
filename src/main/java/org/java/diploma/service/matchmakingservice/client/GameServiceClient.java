package org.java.diploma.service.matchmakingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Creates a <strong>new</strong> match in game-service for each pairing (fresh DB + Redis state).
 * Uses GET /matches/{id} (permitAll on game-service) to drop stale Redis assignments.
 */
@Component
public class GameServiceClient {

    private static final Logger log = LoggerFactory.getLogger(GameServiceClient.class);

    private final RestClient restClient;

    public GameServiceClient(@Value("${game.service.base-url}") String baseUrl) {
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restClient = RestClient.builder().baseUrl(root).build();
    }

    public int createMatch(List<Long> playerIds) {
        try {
            CreateMatchBody body = new CreateMatchBody(playerIds);
            MatchCreated res = restClient.post()
                    .uri("/api/game/matches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MatchCreated.class);
            if (res == null || res.matchId() == null) {
                throw new IllegalStateException("game-service returned empty match body");
            }
            log.info("Created new game matchId={} for players {}", res.matchId(), playerIds);
            return res.matchId();
        } catch (RestClientException e) {
            log.error("game-service create match failed: {}", e.getMessage());
            throw new IllegalStateException("Could not create match in game-service: " + e.getMessage(), e);
        }
    }

    /**
     * @return true if the match exists and includes {@code userId} as a player
     */
    public boolean matchContainsPlayer(int matchId, long userId) {
        try {
            MatchPlayersView m = restClient.get()
                    .uri("/api/game/matches/{id}", matchId)
                    .retrieve()
                    .body(MatchPlayersView.class);
            if (m == null || m.players() == null) {
                return false;
            }
            return m.players().contains(userId);
        } catch (RestClientException e) {
            log.debug("Could not load match {} for validation: {}", matchId, e.getMessage());
            return false;
        }
    }

    public record CreateMatchBody(List<Long> playerIds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchCreated(Integer matchId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchPlayersView(Integer matchId, List<Long> players) {}
}
