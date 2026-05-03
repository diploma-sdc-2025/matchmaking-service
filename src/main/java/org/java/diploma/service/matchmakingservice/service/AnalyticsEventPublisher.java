package org.java.diploma.service.matchmakingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AnalyticsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventPublisher.class);
    private static final String ANALYTICS_CHANNEL = "analytics:events";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AnalyticsEventPublisher(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void publish(String type, Long userId, Integer queueSize, Long matchId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("userId", userId);
        event.put("queueSize", queueSize);
        event.put("matchId", matchId);
        event.put("timestamp", Instant.now());
        try {
            String payload = objectMapper.writeValueAsString(event);
            redis.convertAndSend(ANALYTICS_CHANNEL, payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish analytics event type={} userId={}", type, userId, e);
        }
    }
}

