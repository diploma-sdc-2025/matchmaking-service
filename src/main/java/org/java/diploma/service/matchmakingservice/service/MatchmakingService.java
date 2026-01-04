package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.dto.QueueJoinResponse;
import org.java.diploma.service.matchmakingservice.dto.QueueStatusResponse;
import org.java.diploma.service.matchmakingservice.entity.MatchmakingHistory;
import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.java.diploma.service.matchmakingservice.repository.MatchmakingHistoryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MatchmakingService {

    private static final String QUEUE_KEY = "queue:active";

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
    }

    @Transactional
    public QueueJoinResponse joinQueue(Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        Instant now = Instant.now();

        Double existingScore = redis.opsForZSet()
                .score(QUEUE_KEY, userId.toString());

        if (existingScore != null) {
            Long queueSize = redis.opsForZSet().zCard(QUEUE_KEY);

            return new QueueJoinResponse(
                    "ALREADY_IN_QUEUE",
                    userId,
                    queueSize != null ? queueSize : 0L,
                    Instant.ofEpochMilli(existingScore.longValue())
            );
        }

        redis.opsForZSet()
                .add(QUEUE_KEY, userId.toString(), System.currentTimeMillis());

        MatchmakingHistory history = new MatchmakingHistory();
        history.setUserId(userId);
        history.setJoinedAt(now);
        history.setStatus("WAITING");
        historyRepo.save(history);

        publishEvent("player_join", userId);

        Long queueSize = redis.opsForZSet().zCard(QUEUE_KEY);

        return new QueueJoinResponse(
                "JOINED",
                userId,
                queueSize != null ? queueSize : 0L,
                now
        );
    }

    @Transactional
    public void leaveQueue() {
        Long userId = currentUserId();
        Instant now = Instant.now();

        redis.opsForZSet().remove(QUEUE_KEY, userId.toString());

        historyRepo.findFirstByUserIdAndStatus(userId, "WAITING")
                .ifPresent(h -> {
                    h.setLeftQueueAt(now);
                    h.setStatus("CANCELLED");
                    h.setWaitTimeSeconds(
                            (int) (now.getEpochSecond() - h.getJoinedAt().getEpochSecond())
                    );
                    historyRepo.save(h);
                });

        publishEvent("player_leave", userId);
    }

    private void publishEvent(String type, Long userId) {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType(type);
        event.setUserId(userId);
        event.setQueueSize(getQueueSize());
        event.setTimestamp(Instant.now());
        publisher.publish(event);
    }

    private int getQueueSize() {
        Long size = redis.opsForZSet().zCard(QUEUE_KEY);
        return size != null ? size.intValue() : 0;
    }

    private Long currentUserId() {
        return Long.valueOf(
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
                        .toString()
        );
    }

    public QueueStatusResponse getQueueStatus() {
        Long userId = currentUserId();

        Long rank = redis.opsForZSet()
                .rank(QUEUE_KEY, userId.toString());

        Long size = redis.opsForZSet().zCard(QUEUE_KEY);

        boolean inQueue = rank != null;
        Integer position = inQueue ? rank.intValue() + 1 : null;
        Integer queueSize = size != null ? size.intValue() : 0;

        return new QueueStatusResponse(inQueue, position, queueSize);
    }

}
