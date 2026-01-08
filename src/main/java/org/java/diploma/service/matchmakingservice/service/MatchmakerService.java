package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.client.GameServiceClient;
import org.java.diploma.service.matchmakingservice.dto.CreateMatchRequest;
import org.java.diploma.service.matchmakingservice.dto.GameServiceMatchResponse;
import org.java.diploma.service.matchmakingservice.repository.MatchmakingHistoryRepository;
import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class MatchmakerService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakerService.class);

    private static final String QUEUE_KEY = "queue:active";
    private static final int MINIMUM_PLAYERS_FOR_MATCH = 2;
    private static final int PLAYERS_PER_MATCH = 2;
    private static final int DEFAULT_QUEUE_SIZE = 0;
    private static final long MATCHMAKING_INTERVAL = 5000;

    private static final String EVENT_TYPE_MATCH_CREATED = "match_created";
    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_MATCHED = "MATCHED";

    private static final String MATCHMAKING_STARTED = "Starting matchmaking process, queue size: {}";
    private static final String INSUFFICIENT_PLAYERS = "Insufficient players in queue: {} (need at least {})";
    private static final String PLAYERS_MATCHED = "Players matched: player1={}, player2={}";
    private static final String GAME_SERVICE_MATCH_CREATED = "Game Service created match: gameServiceMatchId={}, players: {} and {}";
    private static final String MATCHMAKING_COMPLETED = "Matchmaking cycle completed, {} matches created";
    private static final String MATCHMAKING_ERROR = "Error during matchmaking process: {}";
    private static final String PLAYER_REMOVED_FROM_QUEUE = "Removed player {} from queue";
    private static final String HISTORY_UPDATED_TO_MATCHED = "Updated matchmaking history for player {} to MATCHED with matchId={}";
    private static final String GAME_SERVICE_CALL_FAILED = "Failed to create match in Game Service for players {} and {}: {}";
    private static final String MATCHMAKER_INITIALIZED = "MatchmakerService initialized";
    private static final String INSUFFICIENT_PLAYERS_IN_SET = "Insufficient players retrieved from queue set";
    private static final String EVENT_PUBLISHED = "Published match_created event for players {} and {}";

    private final RedisTemplate<String, Object> redis;
    private final MatchmakingHistoryRepository historyRepo;
    private final GameServiceClient gameServiceClient;
    private final RedisPublisher publisher;

    public MatchmakerService(
            RedisTemplate<String, Object> redis,
            MatchmakingHistoryRepository historyRepo,
            GameServiceClient gameServiceClient,
            RedisPublisher publisher
    ) {
        this.redis = redis;
        this.historyRepo = historyRepo;
        this.gameServiceClient = gameServiceClient;
        this.publisher = publisher;
        logger.info(MATCHMAKER_INITIALIZED);
    }

    @Scheduled(fixedDelay = MATCHMAKING_INTERVAL)
    @Transactional
    public void processQueue() {
        try {
            Long queueSize = redis.opsForZSet().zCard(QUEUE_KEY);
            int size = queueSize != null ? queueSize.intValue() : DEFAULT_QUEUE_SIZE;

            logger.debug(MATCHMAKING_STARTED, size);

            if (size < MINIMUM_PLAYERS_FOR_MATCH) {
                logger.debug(INSUFFICIENT_PLAYERS, size, MINIMUM_PLAYERS_FOR_MATCH);
                return;
            }

            int matchesCreated = 0;

            while (getQueueSize() >= PLAYERS_PER_MATCH) {
                Set<Object> players = redis.opsForZSet().range(QUEUE_KEY, 0, PLAYERS_PER_MATCH - 1);

                if (players == null || players.size() < PLAYERS_PER_MATCH) {
                    logger.debug(INSUFFICIENT_PLAYERS_IN_SET);
                    break;
                }

                Object[] playerArray = players.toArray();
                Long player1Id = Long.parseLong(playerArray[0].toString());
                Long player2Id = Long.parseLong(playerArray[1].toString());

                logger.info(PLAYERS_MATCHED, player1Id, player2Id);

                boolean matchCreated = createAndNotifyMatch(player1Id, player2Id);

                if (matchCreated) {
                    matchesCreated++;
                }
            }

            logger.info(MATCHMAKING_COMPLETED, matchesCreated);

        } catch (Exception ex) {
            logger.error(MATCHMAKING_ERROR, ex.getMessage(), ex);
        }
    }

    private boolean createAndNotifyMatch(Long player1Id, Long player2Id) {
        try {
            CreateMatchRequest request = new CreateMatchRequest(
                    player1Id,
                    player2Id,
                    null
            );

            GameServiceMatchResponse response = gameServiceClient.createMatch(request);

            logger.info(GAME_SERVICE_MATCH_CREATED, response.getMatchId(), player1Id, player2Id);

            removePlayersFromQueue(player1Id, player2Id);

            updateMatchmakingHistory(player1Id, player2Id, response.getMatchId());

            publishMatchEvent(player1Id, player2Id);

            return true;

        } catch (Exception ex) {
            logger.error(GAME_SERVICE_CALL_FAILED, player1Id, player2Id, ex.getMessage(), ex);
            return false;
        }
    }

    private void removePlayersFromQueue(Long player1Id, Long player2Id) {
        redis.opsForZSet().remove(QUEUE_KEY, player1Id.toString());
        redis.opsForZSet().remove(QUEUE_KEY, player2Id.toString());

        logger.debug(PLAYER_REMOVED_FROM_QUEUE, player1Id);
        logger.debug(PLAYER_REMOVED_FROM_QUEUE, player2Id);
    }

    private void updateMatchmakingHistory(Long player1Id, Long player2Id, Long gameServiceMatchId) {
        Instant now = Instant.now();

        updatePlayerHistory(player1Id, gameServiceMatchId, now);
        updatePlayerHistory(player2Id, gameServiceMatchId, now);
    }

    private void updatePlayerHistory(Long playerId, Long gameServiceMatchId, Instant matchedAt) {
        historyRepo.findFirstByUserIdAndStatus(playerId, STATUS_WAITING)
                .ifPresent(history -> {
                    history.setMatchedAt(matchedAt);
                    history.setMatchId(gameServiceMatchId);
                    history.setStatus(STATUS_MATCHED);
                    history.setWaitTimeSeconds(
                            (int) (matchedAt.getEpochSecond() - history.getJoinedAt().getEpochSecond())
                    );
                    historyRepo.save(history);

                    logger.debug(HISTORY_UPDATED_TO_MATCHED, playerId, gameServiceMatchId);
                });
    }

    private void publishMatchEvent(Long player1Id, Long player2Id) {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType(EVENT_TYPE_MATCH_CREATED);
        event.setUserId(player1Id);
        event.setQueueSize(getQueueSize());
        event.setTimestamp(Instant.now());
        publisher.publish(event);

        logger.debug(EVENT_PUBLISHED, player1Id, player2Id);
    }

    private int getQueueSize() {
        Long size = redis.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size.intValue() : DEFAULT_QUEUE_SIZE;
    }
}
