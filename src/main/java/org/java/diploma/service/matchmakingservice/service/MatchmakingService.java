package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.dto.QueueJoinResponse;
import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.entity.MatchmakingHistory;
import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.java.diploma.service.matchmakingservice.repository.MatchmakingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private static final String QUEUE_KEY = "queue:active";
    private static final int POSITION_OFFSET = 1;
    private static final int DEFAULT_QUEUE_SIZE = 0;
    private static final long DEFAULT_QUEUE_SIZE_LONG = 0L;

    private static final String STATUS_WAITING = "WAITING";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String EVENT_TYPE_PLAYER_JOIN = "player_join";
    private static final String EVENT_TYPE_PLAYER_LEAVE = "player_leave";

    private static final String RESPONSE_STATUS_JOINED = "JOINED";
    private static final String RESPONSE_STATUS_ALREADY_IN_QUEUE = "ALREADY_IN_QUEUE";

    private static final String USER_JOINED_QUEUE = "User {} joined matchmaking queue at position {}";
    private static final String USER_ALREADY_IN_QUEUE = "User {} attempted to join queue but is already in queue";
    private static final String USER_LEFT_QUEUE = "User {} left matchmaking queue after {} seconds";
    private static final String USER_NOT_IN_QUEUE = "User {} attempted to leave queue but was not in queue";
    private static final String QUEUE_STATUS_RETRIEVED = "Queue status retrieved for user {}: inQueue={}, position={}, queueSize={}";
    private static final String EVENT_PUBLISHED = "Published {} event for user {}, queue size: {}";
    private static final String HISTORY_RECORD_CREATED = "Created matchmaking history record for user {}";
    private static final String HISTORY_RECORD_UPDATED = "Updated matchmaking history record for user {} with status {}";
    private static final String SERVICE_INITIALIZED = "MatchmakingService initialized";

    private final RedisTemplate<String, Object> redis;
    private final RedisPublisher publisher;
    private final MatchmakingHistoryRepository historyRepo;

    public MatchmakingService(
            RedisTemplate<String, Object> redis,
            RedisPublisher publisher,
            MatchmakingHistoryRepository historyRepo
    ) {
        this.redis = redis;
        this.publisher = publisher;
        this.historyRepo = historyRepo;
        logger.info(SERVICE_INITIALIZED);
    }

    @Transactional
    public QueueJoinResponse joinQueue(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Instant now = Instant.now();

        Double existingScore = redis.opsForZSet().score(QUEUE_KEY, userId.toString());

        if (existingScore != null) {
            logger.debug(USER_ALREADY_IN_QUEUE, userId);

            Long queueSize = redis.opsForZSet().zCard(QUEUE_KEY);

            return new QueueJoinResponse(
                    RESPONSE_STATUS_ALREADY_IN_QUEUE,
                    userId,
                    queueSize != null ? queueSize : DEFAULT_QUEUE_SIZE_LONG,
                    Instant.ofEpochMilli(existingScore.longValue())
            );
        }

        double timestamp = System.currentTimeMillis();
        redis.opsForZSet().add(QUEUE_KEY, userId.toString(), timestamp);

        MatchmakingHistory history = new MatchmakingHistory();
        history.setUserId(userId);
        history.setJoinedAt(now);
        history.setStatus(STATUS_WAITING);
        historyRepo.save(history);

        logger.info(HISTORY_RECORD_CREATED, userId);

        Long queueSize = redis.opsForZSet().zCard(QUEUE_KEY);
        Long rank = redis.opsForZSet().rank(QUEUE_KEY, userId.toString());
        int position = rank != null ? rank.intValue() + POSITION_OFFSET : (queueSize != null ? queueSize.intValue() : 1);

        logger.info(USER_JOINED_QUEUE, userId, position);

        publishEvent(EVENT_TYPE_PLAYER_JOIN, userId);

        return new QueueJoinResponse(
                RESPONSE_STATUS_JOINED,
                userId,
                queueSize != null ? queueSize : DEFAULT_QUEUE_SIZE_LONG,
                now
        );
    }

    @Transactional
    public void leaveQueue(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Instant now = Instant.now();

        Long removed = redis.opsForZSet().remove(QUEUE_KEY, userId.toString());

        if (removed == null || removed == 0) {
            logger.debug(USER_NOT_IN_QUEUE, userId);
        }

        historyRepo.findFirstByUserIdAndStatus(userId, STATUS_WAITING)
                .ifPresent(h -> {
                    h.setLeftQueueAt(now);
                    h.setStatus(STATUS_CANCELLED);
                    int waitTimeSeconds = (int) (now.getEpochSecond() - h.getJoinedAt().getEpochSecond());
                    h.setWaitTimeSeconds(waitTimeSeconds);
                    historyRepo.save(h);

                    logger.info(USER_LEFT_QUEUE, userId, waitTimeSeconds);
                    logger.debug(HISTORY_RECORD_UPDATED, userId, STATUS_CANCELLED);
                });

        publishEvent(EVENT_TYPE_PLAYER_LEAVE, userId);
    }

    public QueueStatusResponse getQueueStatus(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        Long rank = redis.opsForZSet().rank(QUEUE_KEY, userId.toString());
        Long size = redis.opsForZSet().zCard(QUEUE_KEY);

        boolean inQueue = rank != null;
        Integer position = inQueue ? rank.intValue() + POSITION_OFFSET : null;
        Integer queueSize = size != null ? size.intValue() : DEFAULT_QUEUE_SIZE;

        logger.debug(QUEUE_STATUS_RETRIEVED, userId, inQueue, position, queueSize);

        return new QueueStatusResponse(inQueue, position, queueSize);
    }

    private void publishEvent(String type, Long userId) {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType(type);
        event.setUserId(userId);
        int queueSize = getQueueSize();
        event.setQueueSize(queueSize);
        event.setTimestamp(Instant.now());
        publisher.publish(event);

        logger.debug(EVENT_PUBLISHED, type, userId, queueSize);
    }

    private int getQueueSize() {
        Long size = redis.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size.intValue() : DEFAULT_QUEUE_SIZE;
    }
}