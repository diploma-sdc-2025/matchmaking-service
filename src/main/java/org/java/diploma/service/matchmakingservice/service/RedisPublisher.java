package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "analytics:events";

    public RedisPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(MatchmakingEvent event) {
        redisTemplate.convertAndSend(CHANNEL, event);
    }
}
