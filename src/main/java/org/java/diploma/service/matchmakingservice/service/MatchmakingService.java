package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MatchmakingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisPublisher redisPublisher;

    public MatchmakingService(RedisTemplate<String, Object> redisTemplate, RedisPublisher redisPublisher) {
        this.redisTemplate = redisTemplate;
        this.redisPublisher = redisPublisher;
    }

    public void playerJoinedQueue(Long userId) {
        redisTemplate.opsForZSet().add("queue:active", userId.toString(), System.currentTimeMillis());

        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("player_join");
        event.setUserId(userId);
        event.setQueueSize(getCurrentQueueSize());
        event.setTimestamp(Instant.now());

        redisPublisher.publish(event);
    }

    private int getCurrentQueueSize() {
        Long size = redisTemplate.opsForZSet().zCard("queue:active");
        return size != null ? size.intValue() : 0;
    }
}
