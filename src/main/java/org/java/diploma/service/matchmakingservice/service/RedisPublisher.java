package org.java.diploma.service.matchmakingservice.service;

import org.java.diploma.service.matchmakingservice.event.MatchmakingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisPublisher.class);

    private static final String CHANNEL = "analytics:events";

    private static final String EVENT_PUBLISHED_SUCCESS = "Successfully published event to channel {}: type={}, userId={}";
    private static final String EVENT_PUBLISH_FAILED = "Failed to publish event to channel {}: {}";
    private static final String PUBLISHER_INITIALIZED = "RedisPublisher initialized with channel: {}";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info(PUBLISHER_INITIALIZED, CHANNEL);
    }

    public void publish(MatchmakingEvent event) {
        try {
            redisTemplate.convertAndSend(CHANNEL, event);
            logger.debug(EVENT_PUBLISHED_SUCCESS, CHANNEL, event.getType(), event.getUserId());
        } catch (Exception ex) {
            logger.error(EVENT_PUBLISH_FAILED, CHANNEL, ex.getMessage(), ex);
        }
    }
}